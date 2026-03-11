package com.elimupredict.common;

import com.elimupredict.auth.enums.Role;
import com.elimupredict.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
       seedUser("ADMIN001","System Admin","admin@123", Role.ADMIN);
       seedUser("ITH001","IT Handler","ithandler@123", Role.IT_HANDLER);
    }
    private  void seedUser(String username, String fullName, String password, Role role) {
        if (userRepository.existsByUserName(username)) {
            log.info("User {} already exists. Skipping seeding.", username);
            return;
        }

        userRepository.save(
                com.elimupredict.auth.user.User.builder()
                        .userName(username)
                        .fullName(fullName)
                        .password(passwordEncoder.encode(password))
                        .role(role)
                        .isActive(true)
                        .createdBy("SYSTEM")
                        .build()
        );
        log.info("User {} created successfully.", username);
    }
}
