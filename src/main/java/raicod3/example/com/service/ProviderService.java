package raicod3.example.com.service;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.dto.admin.KycRejectionDto;
import raicod3.example.com.dto.email.EmailRequest;
import raicod3.example.com.dto.provider.*;
import raicod3.example.com.dto.rating.RatingResponseDto;
import raicod3.example.com.enums.ProviderStatus;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.exception.UnauthorizedException;
import raicod3.example.com.lib.rabbitmq.RabbitMQProducer;
import raicod3.example.com.model.ProviderCredits;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.model.User;
import raicod3.example.com.model.UserAddress;
import raicod3.example.com.repository.ProviderCreditsRepository;
import raicod3.example.com.repository.ProviderProfileRepository;
import raicod3.example.com.repository.RatingRepository;
import raicod3.example.com.repository.UserRepository;
import raicod3.example.com.utilities.APIResponse;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderService {
    private final ProviderProfileRepository providerRepository;
    private final UserRepository userRepository;
    private final ProviderCreditsRepository providerCreditsRepository;
    private final Cloudinary cloudinary;
    private final RabbitMQProducer rabbitMQProducer;
    private final RatingRepository ratingRepository;

    @Transactional(readOnly = true)
    public ProviderResponseDto findByUserId(UUID id) {
        // 1. Fetch the profile
        ProviderProfile provider = providerRepository.findByUserId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        // 2. Fetch the credits (financial state)
        // We use orElse(null) because if a user just signed up, the trigger to create
        // their credits row might not have fired yet. The DTO handles the null gracefully.
        ProviderCredits credits = providerCreditsRepository.findById(provider.getId())
                .orElse(null);

        // 3. Pass both to the DTO
        return new ProviderResponseDto(provider, provider.getUser(), credits);
    }

    public ProviderCreditsResponseDto getProviderCredits(UUID providerId) {
        log.debug("Fetching provider with ID: {}", providerId);
        ProviderProfile provider = providerRepository.findByUserId(providerId).orElseThrow(() -> new ResourceNotFoundException("Provider not found"));


        log.debug("Fetching provider credits...");
        ProviderCredits credits =  providerCreditsRepository.findById(provider.getId()).orElseThrow(() -> new ResourceNotFoundException("provider credits not found"));

        return new ProviderCreditsResponseDto(provider.getId(), credits.getBalance(), credits.getActiveTier(), credits.getCreatedAt(), credits.getUpdatedAt(), credits.getSubscriptionExpiresAt());
    }

    @Auditable(action = "PROVIDER_PERSONAL_DETAILS")
    @Transactional
    public APIResponse updateProviderProfile (OnboardingProviderRequestDto dto, String email) {
        log.debug("Validating user...");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Unauthorized. user not found"));

        log.debug("Updating provider's user details...");
        user.setImageUrl(dto.getImageUrl());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setOnboarded(true);

        // --- NEW: Handle User Address Mapping ---
        log.debug("Updating user address...");
        UserAddress address = user.getUserAddress();
        if (address == null) {
            // If they don't have an address record yet, create one
            address = new UserAddress();
            address.setUser(user);
            user.setUserAddress(address); // Link it back to the user for the cascade
        }
        // Update the coordinates and formatted string
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());
        address.setFormattedAddress(dto.getAddress());

        log.debug("Fetching pre-existing provider details...");
        ProviderProfile providerProfile = providerRepository.findByUserId(user.getId()).orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        // This now ONLY updates bio, services, rates, etc.
        providerProfile.updateFromDto(dto);

        // Because of CascadeType.ALL on UserAddress, saving the User saves the Address too.
        userRepository.save(user);
        providerRepository.save(providerProfile);

        // --- NEW: Fetch credits to satisfy the updated ProviderResponseDto ---
        // Using orElse(null) ensures that if the wallet doesn't exist yet, the app doesn't crash.
        ProviderCredits credits = providerCreditsRepository.findById(providerProfile.getId()).orElse(null);

        log.info("Updating provider profile details successful.");

        return APIResponse.success(new ProviderResponseDto(providerProfile, user, credits), "Updated provider personal details", 200);
    }

    @Transactional
    public APIResponse submitKycDocuments(UUID userId, KycSubmissionRequestDto request) {
        // 1. Fetch the provider profile linked to the currently authenticated user
        ProviderProfile profile = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Provider profile not found for this user"));

        // 2. State Machine Guard: Only allow submission if they are new (DRAFT) or fixing an error (REJECTED)
        if (profile.getStatus() != ProviderStatus.DRAFT && profile.getStatus() != ProviderStatus.REJECTED) {
            throw new IllegalStateException("KYC submission not allowed in current state: " + profile.getStatus());
        }

        // 3. Store the secure Cloudinary public_ids
        profile.setCitizenshipFrontId(request.getCitizenshipFrontId());
        profile.setCitizenshipBackId(request.getCitizenshipBackId());

        // 4. Transition the state to lock the provider out of high-value actions until admin review
        profile.setStatus(ProviderStatus.PENDING_APPROVAL);

        // 5. Clear any previous admin rejection notes so they don't pollute the new review
        profile.setRejectionReason(null);

        // 6. Save the entity (JPA tracks changes inside @Transactional, but explicit save is good practice)
        providerRepository.save(profile);

        return APIResponse.success(null, "KYC documents submitted successfully. Account is now pending review.", 200);
    }

    @Transactional
    public APIResponse approveProviderKyc(UUID providerId) {
        ProviderProfile profile = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        if (profile.getStatus() != ProviderStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Cannot approve provider in state: " + profile.getStatus());
        }

        profile.setStatus(ProviderStatus.APPROVED);
        profile.setIsVerified(true);
        providerRepository.save(profile);

        // Build and send the Approval email
        EmailRequest emailReq = new EmailRequest();
        emailReq.setEmail(profile.getUser().getEmail());
        emailReq.setSubject("Congratulations! Your Sewalo Account is Approved");
        emailReq.setTemplatePath("email/kyc-approved");

        rabbitMQProducer.sendEmailNotification(emailReq);

        return APIResponse.success(null, "Provider approved successfully.", 200);
    }

    @Transactional
    public APIResponse rejectProviderKyc(UUID providerId, KycRejectionDto request) {
        ProviderProfile profile = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        if (profile.getStatus() != ProviderStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Cannot reject provider in state: " + profile.getStatus());
        }

        profile.setStatus(ProviderStatus.REJECTED);
        profile.setRejectionReason(request.getReason());
        providerRepository.save(profile);

        // Build and send the Rejection email
        EmailRequest emailReq = new EmailRequest();
        emailReq.setEmail(profile.getUser().getEmail());
        emailReq.setSubject("Action Required: Sewalo Account Verification Failed");
        emailReq.setTemplatePath("email/kyc-rejected");

        // Push the rejection reason into the description field!
        emailReq.setDescription(request.getReason());

        rabbitMQProducer.sendEmailNotification(emailReq);

        return APIResponse.success(null, "Provider KYC rejected.", 200);
    }

    @Transactional(readOnly = true)
    public PublicProviderProfileDto getPublicProfile(UUID providerId) {
        ProviderProfile provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        Double avgRating = ratingRepository.findAverageScoreByProviderId(providerId);
        long ratingCount = ratingRepository.countByProvider_Id(providerId);

        List<RatingResponseDto> recentReviews = ratingRepository
                .findTop10ByProvider_IdOrderByCreatedAtDesc(providerId)
                .stream()
                .map(RatingResponseDto::from)
                .toList();

        String activeTier = providerCreditsRepository.findById(providerId)
                .map(credits -> credits.getActiveTier() != null ? credits.getActiveTier().name() : "FREE")
                .orElse("FREE");

        return new PublicProviderProfileDto(
                provider.getId(),
                provider.getUser().getFullName(),
                provider.getUser().getImageUrl(),
                provider.getServices() != null ? new java.util.ArrayList<>(provider.getServices()) : new java.util.ArrayList<>(),
                avgRating,
                ratingCount,
                recentReviews,
                activeTier
        );
    }

    @Transactional
    @Auditable(action = "PROVIDER_UPDATE_PROFILE")
    public ProviderResponseDto updateProfile(UUID userId, ProviderProfileUpdateDto dto) {

        ProviderProfile provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        User user = provider.getUser();
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setImageUrl(dto.getImageUrl());

        provider.updateFromProfileDto(dto);

        UserAddress address = user.getUserAddress();
        if (address == null) {
            address = new UserAddress();
            address.setUser(user);
            user.setUserAddress(address);
        }
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());
        address.setFormattedAddress(dto.getAddress());

        providerRepository.save(provider);
        userRepository.save(user);

        ProviderCredits credits = providerCreditsRepository.findById(provider.getId()).orElse(null);

        return new ProviderResponseDto(provider, user, credits);
    }

    // Helper method to generate a temporary, signed URL for Admin viewing
    public String generateSignedKycUrl(String publicId) {
        if (publicId == null || publicId.isEmpty()) return null;

        // Generates a URL valid for strict, authenticated access, automatically signed using your API Secret
        return cloudinary.url()
                .resourceType("image")
                .type("authenticated")
                .signed(true)
                .generate(publicId);
    }


}
