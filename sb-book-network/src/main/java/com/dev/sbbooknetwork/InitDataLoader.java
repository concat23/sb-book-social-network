package com.dev.sbbooknetwork;

import com.dev.sbbooknetwork.role.Role;
import com.dev.sbbooknetwork.role.RoleRepository;
import com.dev.sbbooknetwork.user.User;
import com.dev.sbbooknetwork.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class InitDataLoader implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public InitDataLoader(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (roleRepository.findByName("USER").isEmpty()) {
            roleRepository.save(Role.builder().name("USER").build());
        }

        if (roleRepository.findByName("ADMIN").isEmpty()) {
            roleRepository.save(Role.builder().name("ADMIN").build());
        }

        if (userRepository.findByEmail("anhbangluckystar@gmail.com").isEmpty()) {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow(() -> new IllegalStateException("ADMIN role not found"));

            userRepository.save(
                    User.builder()
                            .email("anhbangluckystar@gmail.com")
                            .enabled(true)
                            .password(passwordEncoder.encode("abc@#123"))
                            .roles(Collections.singletonList(adminRole))
                            .build()
            );
        }
    }
}
