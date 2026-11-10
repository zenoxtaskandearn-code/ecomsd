package com.fetch.backend.config;

import com.fetch.backend.user.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Optional local/bootstrap helper. Set APP_ADMIN_BOOTSTRAP_EMAIL to promote an existing registered user to ADMIN.
 * This does not create users or passwords.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private final UserRepository userRepository;
    private final String bootstrapEmail;

    public AdminBootstrap(UserRepository userRepository,
                          @Value("${app.admin.bootstrap-email:}") String bootstrapEmail) {
        this.userRepository = userRepository;
        this.bootstrapEmail = bootstrapEmail;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bootstrapEmail == null || bootstrapEmail.isBlank()) return;
        userRepository.findByEmailIgnoreCase(bootstrapEmail.trim()).ifPresentOrElse(user -> {
            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                user.setRole("ADMIN");
                userRepository.save(user);
                log.info("Promoted bootstrap admin user: {}", user.getEmail());
            }
        }, () -> log.warn("APP_ADMIN_BOOTSTRAP_EMAIL set but no user found for {}", bootstrapEmail));
    }
}
