package raicod3.example.com.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.custom.CustomUserDetails;
import raicod3.example.com.dto.customer.UpdateCustomerProfileRequest;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.model.User;
import raicod3.example.com.model.UserAddress;
import raicod3.example.com.repository.UserRepository;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;


    public UserService (UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);

        return users.map(UserResponseDto::new);
    }

    public UserResponseDto loggedInUser(CustomUserDetails customUserDetails) {
        User user = userRepository.findUserByEmail(customUserDetails.getUsername()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new UserResponseDto(user);
    }

    public UserResponseDto getUserById(UUID id) {

        User existingUser = userRepository.findById(id).orElseThrow(() ->  new ResourceNotFoundException("User not found."));

        return new UserResponseDto(existingUser);
    }

    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found."));

        return new UserResponseDto(user);
    }

    @Transactional
    public UserResponseDto updateCustomerProfile(UUID userId, UpdateCustomerProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only check uniqueness if a phone number was actually provided,
        // and exclude the current user's own row from the check.
        if (request.getPhoneNumber() != null
                && userRepository.existsByPhoneNumberAndIdNot(request.getPhoneNumber(), userId)) {
            throw new DataIntegrityViolationException("Phone number already in use");
        }

        // Safely update basic fields
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getImageUrl() != null) {
            user.setImageUrl(request.getImageUrl());
        }

        // Safely update Address relationship
        if (request.getLatitude() != null && request.getLongitude() != null) {
            UserAddress address = user.getUserAddress();
            if (address == null) {
                address = new UserAddress();
                address.setUser(user);
            }
            address.setLatitude(request.getLatitude());
            address.setLongitude(request.getLongitude());
            address.setFormattedAddress(request.getAddress());
            user.setUserAddress(address);
        }

        if (!user.isOnboarded()) {
            user.setOnboarded(true);
        }

        User savedUser = userRepository.save(user);
        return new UserResponseDto(savedUser);
    }
}
