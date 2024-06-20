package com.dev.sbbooknetwork.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AuthenticationResponse {
    private String token;
    private LocalDateTime loginTime;
    private String loginPage;
}
