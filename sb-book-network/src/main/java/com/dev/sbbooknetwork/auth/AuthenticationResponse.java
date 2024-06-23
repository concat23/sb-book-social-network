package com.dev.sbbooknetwork.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AuthenticationResponse {

    private String message;
    private String token;
    private LocalDateTime loginTime;
    private String loginPage;
    private int loginAttempts;

    public AuthenticationResponse(String message) {
        this.message = message;
    }

    public AuthenticationResponse(String token, LocalDateTime loginTime, String loginPage, int loginAttempts) {
        this.token = token;
        this.loginTime = loginTime;
        this.loginPage = loginPage;
        this.loginAttempts = loginAttempts;
    }

    // Add this constructor if needed
    public AuthenticationResponse(String message, String token, LocalDateTime loginTime, String loginPage,int loginAttempts) {
        this.message = message;
        this.token = token;
        this.loginTime = loginTime;
        this.loginPage = loginPage;
        this.loginAttempts = loginAttempts;

    }
}
