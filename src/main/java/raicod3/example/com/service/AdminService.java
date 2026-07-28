package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.dto.admin.PendingProviderDto;
import raicod3.example.com.enums.ProviderStatus;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.repository.ProviderProfileRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderService providerService;


    @Transactional(readOnly = true)
    public List<PendingProviderDto> getPendingProviders() {
        List<ProviderProfile> pendingProfiles = providerProfileRepository.findByStatus(ProviderStatus.PENDING_APPROVAL);

        return pendingProfiles.stream().map(profile -> {
            String frontUrl = providerService.generateSignedKycUrl(profile.getCitizenshipFrontId());
            String backUrl = providerService.generateSignedKycUrl(profile.getCitizenshipBackId());

            return PendingProviderDto.builder()
                    .id(profile.getId())
                    .fullName(profile.getUser().getFullName())
                    .email(profile.getUser().getEmail())
                    .phoneNumber(profile.getUser().getPhoneNumber())
                    .citizenshipFrontUrl(frontUrl)
                    .citizenshipBackUrl(backUrl)
                    .build();
        }).collect(Collectors.toList());
    }
}
