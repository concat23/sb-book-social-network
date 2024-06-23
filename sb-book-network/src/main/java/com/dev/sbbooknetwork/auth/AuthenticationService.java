package com.dev.sbbooknetwork.auth;


import com.dev.sbbooknetwork.email.EmailService;
import com.dev.sbbooknetwork.email.EmailTemplateName;
import com.dev.sbbooknetwork.role.RoleRepository;
import com.dev.sbbooknetwork.security.JwtService;
import com.dev.sbbooknetwork.token.Token;
import com.dev.sbbooknetwork.token.TokenRepository;
import com.dev.sbbooknetwork.user.User;
import com.dev.sbbooknetwork.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.xml.bind.DatatypeConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;


@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final TokenRepository tokenRepository;

    private final EmailService emailService;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final AuthenticationAttemptService authenticationAttemptService;

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;
    public void register(RegistrationRequest request) throws MessagingException {

        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new IllegalStateException("Email already taken");
        });

        var userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));
        var user = User.builder()
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .accountLocked(false)
                        .enabled(false)
                        .roles(List.of(userRole))
                        .build();
        userRepository.save(user);
        sendValidationEmail(user);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request, String msg ,LocalDateTime loginTime, String loginPage, int loginAttempts) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var claims = new HashMap<String, Object>();
        var user = (User) auth.getPrincipal();
        claims.put("fullName", user.getUsername());

        var jwtToken = jwtService.generateToken(claims, user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message(msg)
                .loginTime(loginTime)
                .loginPage(loginPage)
                .loginAttempts(loginAttempts)
                .build();
    }


    public void requestPasswordReset(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("User not found with email: " + email);
        }

        User user = optionalUser.get();

        // Generate reset token and expiry time
        String resetToken = generateAndSaveResetToken(user);
        LocalDateTime resetTokenExpiry = LocalDateTime.now().plusHours(1); // Token expires in 1 hour

        // Generate signature for the reset link (example, you can customize this)
        String signature = generateSignature(user.getEmail(), resetToken);

        // Update user with reset token and expiry
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(resetTokenExpiry);
        userRepository.save(user);

        // Send password reset email
        String resetLink = "http://localhost:8600/reset-password?code=" + resetToken + "&signature=" + signature;
        emailService.sendPasswordResetEmail(user, resetLink);
    }

    public void resetPassword(String code, String signature, String newPassword) {
        // Find user by reset token
        Optional<User> optionalUser = userRepository.findByResetToken(code);
        User user = optionalUser.orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        // Validate token signature if necessary
        if (!validateTokenSignature(user, signature)) {
            throw new IllegalArgumentException("Invalid token signature");
        }

        // Validate token expiry
        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expired");
        }

        // Reset password and update user
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.resetLoginAttempts(); // Reset login attempts in the User entity
        userRepository.save(user);

        // Evict user from auth attempt cache
        authenticationAttemptService.evictUserFromAuthAttemptCache(user.getEmail());
    }

    public void resetLoginAttempts(User user) {
        user.setLoginAttempts(0);
        user.setAccountLocked(false);
        userRepository.save(user);
    }


    private void sendValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);
        emailService.sendMail(user.getEmail(), user.fullName(), EmailTemplateName.ACTIVATE_ACCOUNT,activationUrl,newToken, "Account activation");
    }

    private String generateSignature(String email, String resetToken) {

        return email.substring(0, 3) + resetToken.substring(0, 3);
    }

    private String generateAndSaveActivationToken(User user) {
        String generatedToken = generateActivationCode(6);
        var token = Token.builder()
                        .token(generatedToken)
                        .createdAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user).build();
        tokenRepository.save(token);
        return generatedToken;
    }

    private String generateAndSaveResetToken(User user) {
        String generatedToken = generateRandomNumericToken(6); // Generate 6-digit numeric token
        LocalDateTime resetTokenExpiry = LocalDateTime.now().plusHours(1); // Token expires in 1 hour

        Token token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(resetTokenExpiry)
                .user(user)
                .build();

        tokenRepository.save(token);
        return generatedToken;
    }

    private String generateRandomNumericToken(int length) {
        Random random = new Random();
        StringBuilder tokenBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            tokenBuilder.append(random.nextInt(10)); // Append a random digit (0-9)
        }
        return tokenBuilder.toString();
    }

    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();
        for (int i = 0; i < length; i ++){
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }


//    @Transactional
    public boolean activateAccount(String token) throws MessagingException {
        Token savedToken = tokenRepository.findByToken(token).orElseThrow(
                () -> new RuntimeException("Invalid token")
        );

        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())){
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activation token has expired. A new token has been sent to the same email address");
        }

        var user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);

        return true;
    }

    private boolean validateTokenSignature(User user, String signature) {
        // Retrieve the reset token stored in the user object
        String storedToken = user.getResetToken();

        // Generate the expected signature based on the stored token
        String expectedSignature = generateSignature(storedToken);

        // Compare the expected signature with the provided signature
        return expectedSignature.equals(signature);
    }

    private String generateSignature(String resetToken) {

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(resetToken.getBytes(StandardCharsets.UTF_8));
            return DatatypeConverter.printHexBinary(hashBytes).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating token signature", e);
        }
    }
}
