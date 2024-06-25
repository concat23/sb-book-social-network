package com.dev.sbbooknetwork.auth;

import com.dev.sbbooknetwork.email.EmailService;
import com.dev.sbbooknetwork.email.EmailTemplateName;
import com.dev.sbbooknetwork.role.RoleRepository;
import com.dev.sbbooknetwork.security.JwtService;
import com.dev.sbbooknetwork.token.Token;
import com.dev.sbbooknetwork.token.TokenRepository;
import com.dev.sbbooknetwork.user.User;
import com.dev.sbbooknetwork.user.UserRepository;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.mail.MessagingException;
import jakarta.xml.bind.DatatypeConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountLockedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service

public class AuthenticationService {

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    private static final int MAXIMUM_NUMBER_OF_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 5;

    private final LoadingCache<String, Integer> loginAttemptCache;

    private static final int ATTEMPT_INCREMENT = 1;

    private Map<String, Integer> loginAttemptsMap = new HashMap<>();

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;


    public AuthenticationService(RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder,
                                 UserRepository userRepository,
                                 TokenRepository tokenRepository,
                                 EmailService emailService,
                                 AuthenticationManager authenticationManager,
                                 JwtService jwtService) {
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        loginAttemptCache = CacheBuilder.newBuilder()
                .expireAfterWrite(LOCK_DURATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(100)
                .build(new CacheLoader<String, Integer>() {
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }

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

    public AuthenticationResponse authenticate(AuthenticationRequest request, String msg, LocalDateTime loginTime, String loginPage) throws AccountLockedException {
        String email = request.getEmail();
        String password = request.getPassword();
        int count = loginAttemptsMap.getOrDefault(email, 0);

        if (isAccountLocked(email)) {
            throw new AccountLockedException("Account is locked. Please try again later.");
        }

        // Check if the email exists in the user repository
        if (!userRepository.emailExists(email)) {
            return handleInvalidEmail(loginTime, loginPage);
        }

        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            password
                    )
            );

            var claims = new HashMap<String, Object>();
            var user = (User) auth.getPrincipal();
            claims.put("fullName", user.getUsername());

            var jwtToken = jwtService.generateToken(claims, user);

            evictUserFromLoginAttemptCache(email);
            loginAttemptsMap.put(email, 0);  // Reset email attempt count after successful login

            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .message(msg)
                    .loginTime(loginTime)
                    .loginPage(loginPage)
                    .countLoginFailed(0)
                    .build();

        } catch (BadCredentialsException e) {
            count++;  // Increment email attempt count
            loginAttemptsMap.put(email, count);

            if (count > MAXIMUM_NUMBER_OF_ATTEMPTS) {
                lockUserAccountForFiveMinutes(email);  // Lock account if count reaches maximum attempts
                throw new AccountLockedException("Account is locked. Please try again later.");
            }

            // Determine the type of authentication failure
            switch (getAuthenticationFailureType(e)) {
                case INVALID_PASSWORD:
                    return handleInvalidPassword(loginTime, loginPage, count, password);
                case INVALID_CREDENTIALS:
                default:
                    return handleInvalidCredentials(email, loginTime, loginPage, count);
            }
        } catch (AuthenticationException e) {
            // Handle other authentication exceptions if necessary
            count++;
            loginAttemptsMap.put(email, count);

            if (count > MAXIMUM_NUMBER_OF_ATTEMPTS) {
                lockUserAccountForFiveMinutes(email);
                throw new AccountLockedException("Account is locked. Please try again later.");
            }

            return handleAuthenticationFailed(email, loginTime, loginPage, count);
        }
    }

    private AuthenticationResponse handleInvalidEmail(LocalDateTime loginTime, String loginPage) {
        return AuthenticationResponse.builder()
                .message("Email does not exist")
                .loginTime(loginTime)
                .loginPage(loginPage)
                .build();
    }

    private AuthenticationResponse handleInvalidPassword(LocalDateTime loginTime, String loginPage, int count, String password) {
        return AuthenticationResponse.builder()
                .message("Password " + password + " is invalid")
                .loginTime(loginTime)
                .loginPage(loginPage)
                .countLoginFailed(count)
                .build();
    }

    private AuthenticationResponse handleInvalidCredentials(String email, LocalDateTime loginTime, String loginPage, int count) {
        return AuthenticationResponse.builder()
                .message("Invalid credentials")
                .loginTime(loginTime)
                .loginPage(loginPage)
                .countLoginFailed(count)
                .build();
    }

    private AuthenticationResponse handleAuthenticationFailed(String email, LocalDateTime loginTime, String loginPage, int count) {
        return AuthenticationResponse.builder()
                .message("Authentication failed")
                .loginTime(loginTime)
                .loginPage(loginPage)
                .countLoginFailed(count)
                .build();
    }

    private AuthenticationFailureType getAuthenticationFailureType(BadCredentialsException e) {
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("UsernameNotFoundException") || message.contains("UserDetailsService returned null")) {
                return AuthenticationFailureType.INVALID_EMAIL;
            } else if (message.contains("Bad credentials")) {
                return AuthenticationFailureType.INVALID_PASSWORD;
            }
        }
        return AuthenticationFailureType.INVALID_CREDENTIALS;
    }

    private String getCountDescription(int count) {
        switch (count) {
            case 1:
                return "first";
            case 2:
                return "second";
            case 3:
                return "third";
            case 4:
                return "fourth";
            case 5:
                return "fifth";
            default:
                return count + "th";
        }
    }

    private enum AuthenticationFailureType {
        INVALID_EMAIL,
        INVALID_PASSWORD,
        INVALID_CREDENTIALS
    }

    public void evictUserFromLoginAttemptCache(String username) {
        loginAttemptCache.invalidate(username);
    }

    public void addUserToLoginAttemptCache(String username) {
        int attempts = 0;
        try {
            attempts = ATTEMPT_INCREMENT + loginAttemptCache.get(username);
            loginAttemptCache.put(username, attempts);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public boolean isAccountLocked(String username) {
        try {
            return loginAttemptCache.get(username) >= MAXIMUM_NUMBER_OF_ATTEMPTS;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getLoginAttempts(String username) {
        try {
            return loginAttemptCache.get(username);
        } catch (ExecutionException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean lockUserAccountForFiveMinutes(String username) {
        Optional<User> userOptional = userRepository.findByEmail(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            LocalDateTime lockTime = LocalDateTime.now();
            LocalDateTime unlockTime = lockTime.plusMinutes(LOCK_DURATION_MINUTES);

            user.setAccountNonLocked(false);
            user.setLockTime(lockTime);
            user.setUnlockTime(unlockTime);

            userRepository.save(user);

            System.out.println("Account locked for user: " + username + " until " + unlockTime);
            return true; // Khóa thành công
        } else {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
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

        // Generate signature for the reset link
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
            System.out.println("Expected signature: " + generateSignature(user.getResetToken()));
            System.out.println("Received signature: " + signature);
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

        // Unlock the account
        user.setAccountLocked(false);

        userRepository.save(user);
    }


    private void sendValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);
        emailService.sendMail(user.getEmail(), user.fullName(), EmailTemplateName.ACTIVATE_ACCOUNT, activationUrl, newToken, "Account activation");
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
                .user(user)
                .build();
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
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }

    public boolean activateAccount(String token) throws MessagingException {
        Token savedToken = tokenRepository.findByToken(token).orElseThrow(
                () -> new RuntimeException("Invalid token")
        );

        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
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
