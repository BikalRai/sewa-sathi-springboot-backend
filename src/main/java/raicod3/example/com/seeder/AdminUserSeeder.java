package raicod3.example.com.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.enums.UserRole;
import raicod3.example.com.model.User;
import raicod3.example.com.repository.UserRepository;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1) // Runs before JobCategorySeeder or any seeder that might depend on an admin existing
public class AdminUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.email}")
    private String adminEmail;

    @Value("${app.seed.admin.password}")
    private String adminPassword;

    @Value("${app.seed.admin.fullName:System Administrator}")
    private String adminFullName;

    @Override
    @Transactional
    public void run(String... args) {
        // Idempotency Check: scoped to role, not just "any user exists".
        // This means seeding still skips correctly even after customers/providers
        // have signed up — it only cares whether an ADMIN specifically exists.
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            log.info("Admin account already exists. Skipping admin seeding...");
            return;
        }

        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.error("Admin seed credentials are not configured (app.seed.admin.email / app.seed.admin.password). Skipping admin seeding.");
            return;
        }

        log.info("Seeding Admin account...");

        User admin = new User();
        admin.setFullName(adminFullName);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);

        // Admin bypasses the normal onboarding/verification lifecycle entirely —
        // active, onboarded, and unlocked from the moment it's created.
        admin.setActive(true);
        admin.setOnboarded(true);
        admin.setAccountLocked(false);
        admin.setFailedLoginAttempts(0);
        admin.setCreatedAt(LocalDateTime.now());

        // Intentionally NOT setting providerProfile or customerProfile —
        // admin only ever needs the base User row.

        userRepository.save(admin);

        log.info("Successfully seeded Admin account with email: {}", adminEmail);
    }
}