package com.dev.sbbooknetwork.listener;

import com.dev.sbbooknetwork.auth.AuthenticationService;
import com.dev.sbbooknetwork.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class AuthenticationSuccessListener {
    private final AuthenticationService loginAttemptService;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if(principal instanceof User) {
            User user = (User) event.getAuthentication().getPrincipal();
            loginAttemptService.evictUserFromLoginAttemptCache(user.getEmail());
        }
    }
}
