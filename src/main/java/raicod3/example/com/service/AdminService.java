package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.dto.provider.ProviderAdminResponseDto;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.repository.ProviderProfileRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ProviderProfileRepository providerProfileRepository;

    @Transactional
    public ProviderAdminResponseDto verifyProvider(UUID providerId) {
        ProviderProfile provider = providerProfileRepository.findByIdWithServices(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        provider.setIsVerified(true);
        providerProfileRepository.save(provider);

        return new ProviderAdminResponseDto(provider);
    }

    @Transactional(readOnly = true)
    public List<ProviderAdminResponseDto> getPendingProviders() {
        return providerProfileRepository.findByIsVerifiedFalseWithServices()
                .stream()
                .map(ProviderAdminResponseDto::new)
                .toList();
    }
}
