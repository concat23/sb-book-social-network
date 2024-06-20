package com.dev.sbbooknetwork.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;


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


    @PostMapping("/authenticate")
    @Operation(
            summary = "Authenticate user",
            description = "Authenticate a user based on the provided credentials.",
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
                                                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "status": "UNAUTHORIZED",
                                                      "message": "Invalid credentials"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Parameter(description = "Authentication request details", required = true)
            @RequestBody @Valid AuthenticationRequest request){
            String loginPage = String.valueOf(this.getCurrentRequestBaseUri() + "/auth/authenticate");
            LocalDateTime loginTime = LocalDateTime.now();
            AuthenticationResponse response = service.authenticate(request,loginTime,loginPage);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/activate-account")
    public void confirm(
            @RequestParam String token
    ) throws MessagingException {
        service.activateAccount(token);
    }

    private URI getCurrentRequestBaseUri() {
        return ServletUriComponentsBuilder.fromCurrentServletMapping().build().toUri();
    }

}
