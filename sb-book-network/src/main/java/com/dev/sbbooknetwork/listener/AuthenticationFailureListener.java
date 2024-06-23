package com.dev.sbbooknetwork.listener;

import com.dev.sbbooknetwork.auth.AuthenticationAttemptService;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;


@Component
@AllArgsConstructor
public class AuthenticationFailureListener {
    private final AuthenticationAttemptService authAttemptService;

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if(principal instanceof String) {
            String email = (String) event.getAuthentication().getPrincipal();
            authAttemptService.addUserToAuthAttemptCache(email);
        }
    }
}
