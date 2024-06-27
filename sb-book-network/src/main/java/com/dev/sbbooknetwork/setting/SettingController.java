package com.dev.sbbooknetwork.setting;


import com.dev.sbbooknetwork.role.Role;
import com.dev.sbbooknetwork.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("settings")
@RequiredArgsConstructor
@Tag(name="Settings", description = "Endpoints for settings")
public class SettingController {

    private final SettingService service;
    @Operation(summary = "Get users by role name", description = "Returns a list of users based on the role name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved users"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @GetMapping("/{roleName}/users")
    public ResponseEntity<List<User>> getUsersByRole(
            @Parameter(description = "Name of the role", example = "admin") @PathVariable String roleName) {
        return ResponseEntity.ok(service.getUsersByRole(roleName));
    }

    @Operation(summary = "Get all roles", description = "Returns a list of all roles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved roles"),
            @ApiResponse(responseCode = "404", description = "No roles found")
    })
    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getAllRoles() {

        return ResponseEntity.ok(null);
    }
}
