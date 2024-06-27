package com.dev.sbbooknetwork.user;


import com.dev.sbbooknetwork.role.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;


    public List<User> getUsersByRole(String roleName) {
        User currentUser = getCurrentUser();

        if (currentUser != null && currentUser.getRoleNames().contains("ADMIN")) {
            // If current user is ADMIN, return all users
            return userRepository.findAll();
        } else {
            // Otherwise, return users with the specified role
            return userRepository.findUsersByRoleName(roleName);
        }
    }

    public List<Role> getRolesByUser(User user) {
        return user.getRoles();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }
}
