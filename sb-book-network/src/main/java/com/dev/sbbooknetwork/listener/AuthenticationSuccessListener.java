package com.dev.sbbooknetwork.listener;

import com.dev.sbbooknetwork.auth.AuthenticationAttemptService;
import com.dev.sbbooknetwork.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationSuccessListener {


    private final AuthenticationAttemptService authAttemptService;
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if(principal instanceof User) {
            User user = (User) event.getAuthentication().getPrincipal();
            authAttemptService.evictUserFromAuthAttemptCache(user.getEmail());
        }
    }
}
