package com.dev.sbbooknetwork.role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    /**
     * Retrieves a role by its name.
     *
     * @param roleName Name of the role to retrieve.
     * @return Optional containing the role if found, empty otherwise.
     */
    public Optional<Role> getRoleByName(String roleName) {
        return roleRepository.findByName(roleName);
    }

    /**
     * Retrieves all roles.
     *
     * @return List of all roles.
     */
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}
