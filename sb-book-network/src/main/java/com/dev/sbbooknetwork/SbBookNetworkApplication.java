package com.dev.sbbooknetwork;

import com.dev.sbbooknetwork.role.Role;
import com.dev.sbbooknetwork.role.RoleRepository;
import com.dev.sbbooknetwork.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class SbBookNetworkApplication {

    public static void main(String[] args) {
//        SpringApplication.run(SbBookNetworkApplication.class, args);
        ConfigurableApplicationContext context = SpringApplication.run(SbBookNetworkApplication.class, args);

        RoleRepository roleRepository = context.getBean(RoleRepository.class);
        UserRepository userRepository = context.getBean(UserRepository.class);
        PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);

        InitDataLoader dataLoader = new InitDataLoader(roleRepository, userRepository, passwordEncoder);
        dataLoader.run();
    }

}
