package com.banking.identity.service;

import com.banking.identity.domain.Permission;
import com.banking.identity.domain.Role;
import com.banking.identity.domain.UserRole;
import com.banking.identity.repository.PermissionRepository;
import com.banking.identity.repository.RoleRepository;
import com.banking.identity.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;

    public RoleService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRoleRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Role getRole(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    }

    public Role getRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + name));
    }

    @Transactional
    public Role createRole(String name, String description, Set<UUID> permissionIds) {
        if (roleRepository.existsByName(name)) {
            throw new IllegalArgumentException("Role already exists: " + name);
        }

        Role role = new Role();
        role.setName(name);
        role.setDescription(description);

        if (permissionIds != null && !permissionIds.isEmpty()) {
            Set<Permission> permissions = permissionIds.stream()
                    .map(id -> permissionRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id)))
                    .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        log.info("Created role: {}", name);
        return roleRepository.save(role);
    }

    @Transactional
    public void assignRoleToUser(UUID userId, UUID roleId, UUID assignedBy) {
        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new IllegalArgumentException("User already has this role");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRole(role);
        userRole.setAssignedBy(assignedBy);
        userRoleRepository.save(userRole);

        log.info("Assigned role {} to user {}", role.getName(), userId);
    }

    @Transactional
    public void removeRoleFromUser(UUID userId, UUID roleId) {
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        log.info("Removed role {} from user {}", roleId, userId);
    }

    public List<Role> getUserRoles(UUID userId) {
        return userRoleRepository.findActiveRolesByUserId(userId).stream()
                .map(UserRole::getRole)
                .collect(Collectors.toList());
    }

    public Set<String> getUserPermissions(UUID userId) {
        return getUserRoles(userId).stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }

    public boolean hasPermission(UUID userId, String permissionName) {
        return getUserPermissions(userId).contains(permissionName);
    }

    public boolean hasRole(UUID userId, String roleName) {
        return getUserRoles(userId).stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    // Permission management
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @Transactional
    public Permission createPermission(String name, String resource, String action, String description) {
        if (permissionRepository.existsByName(name)) {
            throw new IllegalArgumentException("Permission already exists: " + name);
        }

        Permission permission = new Permission();
        permission.setName(name);
        permission.setResource(resource);
        permission.setAction(action);
        permission.setDescription(description);

        log.info("Created permission: {}", name);
        return permissionRepository.save(permission);
    }
}
