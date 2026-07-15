package com.marakicode.financetracker.users;

import com.marakicode.financetracker.common.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.admin.initializer.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.countByRole(Role.ADMIN) > 0) {
            log.info("event=admin_init.skipped reason=admin_users_exist");
            return;
        }

        String email = environment.getProperty("ADMIN_EMAIL");
        String password = environment.getProperty("ADMIN_PASSWORD");
        String firstName = environment.getProperty("ADMIN_FIRST_NAME");
        String lastName = environment.getProperty("ADMIN_LAST_NAME");

        if (isMissing(email, password, firstName, lastName)) {
            log.info("event=admin_init.skipped reason=env_vars_missing");
            return;
        }

        String normalizedEmail = EmailNormalizer.normalize(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.warn("event=admin_init.skipped reason=email_already_registered email={}", normalizedEmail);
            return;
        }

        User admin = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .active(true)
                .build();

        try {
            userRepository.save(admin);
            log.info("event=admin_init.success email={}", normalizedEmail);
        } catch (DataIntegrityViolationException e) {
            log.info("event=admin_init.skipped reason=concurrent_init email={}", normalizedEmail);
        }
    }

    private boolean isMissing(String... values) {
        for (String v : values) {
            if (v == null || v.isBlank()) return true;
        }
        return false;
    }
}
