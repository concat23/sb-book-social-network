package com.dev.sbbooknetwork.auth;

import com.dev.sbbooknetwork.user.User;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import javax.security.auth.login.AccountLockedException;
import java.time.LocalDateTime;

import static com.dev.sbbooknetwork.constant.ApiMessage.*;


@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
@Tag(name="Authentication", description = "Endpoints for user authentication and registration")
public class AuthenticationController {
    private final AuthenticationService service;
    @Secured("ADMIN")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            summary = "Register a new user",
            description = "Registers a new user in the system.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RegistrationRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                             "firstName": "Bang",
                                             "lastName": "Vo Anh",
                                             "email": "anhbangluckytar@gmail.com",
                                             "password": "abc@#123"
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "202",
                            description = "User registered successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = RegistrationResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "message": "User registered successfully",
                                                      "status": 202
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request format or data",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "message": "Validation failed",
                                                      "errors": [
                                                        "Username cannot be blank",
                                                        "Email format is invalid"
                                                      ],
                                                      "status": 400
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<?> register(
            @Parameter(description = "Registration request details", required = true)
            @RequestBody @Valid RegistrationRequest request) throws MessagingException {
        service.register(request);
        RegistrationResponse response = new RegistrationResponse("User registered successfully",202);
        return ResponseEntity.accepted().body(response);
    }


    @Operation(
            summary = "Authenticate user",
            description = """
                      Authenticate a user based on the provided credentials.
                      Users are allowed a maximum of 5 failed login attempts before the account is locked.
                      """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthenticationRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "email": "anhbangluckystar@gmail.com",
                                          "password": "abc@#123"
                                        }
                                        """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Authentication successful",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthenticationResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                {
                                                  "message": "Login successfully.",
                                                  "token": "eyJhbGciOiJIUzM4NCJ9.eyJmdWxsTmFtZSI6ImFuaGJhbmdsdWNreXN0YXJAZ21haWwuY29tIiwic3ViIjoiYW5oYmFuZ2x1Y2t5c3RhckBnbWFpbC5jb20iLCJpYXQiOjE3MTkzNDIxMDYsImV4cCI6MTcxOTM1MDc0NiwiYXV0aG9yaXRpZXMiOlsiQURNSU4iXX0.aBDtw51MkiQlz-1EasjkU6QVs1MQTDgZkn2-NB1cRTeYtyAYypn6deDmLz5VRAGg",
                                                  "loginTime": "2024-06-25T19:01:45.496330837",
                                                  "loginPage": "http://localhost:8600/api/v1/auth/authenticate",
                                                  "countLoginFailed": 0
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Error response",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "Invalid credentials",
                                                    value = """
                                                            {
                                                              "status": "UNAUTHORIZED",
                                                              "message": "Invalid credentials"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "Account locked",
                                                    value = """
                                                            {
                                                              "status": "UNAUTHORIZED",
                                                              "message": "Account locked due to too many failed login attempts"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request) throws AccountLockedException {
        String loginPage = getCurrentRequestBaseUri() + "/auth/authenticate";
        LocalDateTime loginTime = LocalDateTime.now();
        try {
            AuthenticationResponse response = service.authenticate(request, LOGIN_SUCCESSFULLY, loginTime, loginPage);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            AuthenticationResponse responseFailed = service.authenticate(request, LOGIN_FAILED, loginTime, loginPage);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseFailed);
        }
    }





    @Operation(
            summary = "Activate user account",
            description = "Activates a user account using the provided activation token."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account activated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ActivationAccountResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "message": "Your account has been activated successfully."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Activation failed due to invalid token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ActivationAccountResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "message": "Activation failed: Invalid token."
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/activate-account")
    public ResponseEntity<ActivationAccountResponse> confirm(@RequestParam String token) throws MessagingException {
        boolean isActivated = service.activateAccount(token);
        ActivationAccountResponse activationAccountResponse;

        if (isActivated) {
            activationAccountResponse = new ActivationAccountResponse(ACCOUNT_ACTIVATED_SUCCESSFULLY);
            return ResponseEntity.ok(activationAccountResponse);
        } else {
            activationAccountResponse = new ActivationAccountResponse(ACTIVATION_FAILED_INVALID_TOKEN);
            return ResponseEntity.badRequest().body(activationAccountResponse);
        }
    }


    @Operation(
            summary = "Request password reset",
            description = "Initiates a password reset process for the given email address."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Password reset request sent successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid email format"
    )
    @PostMapping("/reset-password/request")
    public ResponseEntity<String> requestPasswordReset(@RequestParam("email") String email) {
        service.requestPasswordReset(email);
        return ResponseEntity.ok("Password reset request sent successfully");
    }

    @Operation(
            summary = "Reset password",
            description = "Resets the password using the provided reset token."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Password reset successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid token format or expired token"
    )
    @PostMapping("/reset-password/submit")
    public ResponseEntity<String> resetPassword(@RequestParam("code") String code,
                                                @RequestParam("signature") String signature,
                                                @RequestParam("newPassword") String newPassword) {
        service.resetPassword(code, signature, newPassword);
        return ResponseEntity.ok("Password reset successfully");
    }


    @GetMapping("/current")
    @Operation(summary = "Get current user details", description = "Retrieve the details of the current user based on the token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<String> getCurrentUser(@RequestParam("tokenCode") String tokenCode) {
        User currentUser = service.getUserFromToken(tokenCode);
        if (currentUser != null) {
            String mail = currentUser.getEmail();
            try {
                Gson gson = new Gson();
                String jsonMail = gson.toJson(mail);
                return ResponseEntity.ok(jsonMail);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(500).body("Internal server error: Unable to parse email as JSON");
            }
        } else {
            return ResponseEntity.status(401).build();
        }
    }

//    private URI getCurrentRequestBaseUri() {
//        return ServletUriComponentsBuilder.fromCurrentServletMapping().build().toUri();
//    }
//    private String getCurrentRequestBaseUri() {
//        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
//        String scheme = request.getScheme();
//        String serverName = request.getServerName();
//        int serverPort = request.getServerPort();
//        String contextPath = request.getContextPath();
//        return scheme + "://" + serverName + ":" + serverPort + contextPath;
//    }

    private String getCurrentRequestBaseUri() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return UriComponentsBuilder.fromHttpUrl(request.getRequestURL().toString())
                .replacePath(request.getContextPath()).build().toUriString();
    }
}
