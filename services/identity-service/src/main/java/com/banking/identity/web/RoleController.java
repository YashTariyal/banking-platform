package com.banking.identity.web;

import com.banking.identity.domain.Permission;
import com.banking.identity.domain.Role;
import com.banking.identity.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roles & Permissions", description = "RBAC management APIs")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "List all roles")
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    public ResponseEntity<Role> getRole(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.getRole(id));
    }

    @PostMapping
    @Operation(summary = "Create a new role")
    public ResponseEntity<Role> createRole(@Valid @RequestBody CreateRoleRequest request) {
        Role role = roleService.createRole(request.name(), request.description(), request.permissionIds());
        return ResponseEntity.ok(role);
    }

    @PostMapping("/assign")
    @Operation(summary = "Assign role to user")
    public ResponseEntity<Void> assignRole(@Valid @RequestBody AssignRoleRequest request) {
        roleService.assignRoleToUser(request.userId(), request.roleId(), request.assignedBy());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @Operation(summary = "Remove role from user")
    public ResponseEntity<Void> removeRole(@PathVariable UUID userId, @PathVariable UUID roleId) {
        roleService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user roles")
    public ResponseEntity<List<Role>> getUserRoles(@PathVariable UUID userId) {
        return ResponseEntity.ok(roleService.getUserRoles(userId));
    }

    @GetMapping("/users/{userId}/permissions")
    @Operation(summary = "Get user permissions")
    public ResponseEntity<Set<String>> getUserPermissions(@PathVariable UUID userId) {
        return ResponseEntity.ok(roleService.getUserPermissions(userId));
    }

    @GetMapping("/users/{userId}/check")
    @Operation(summary = "Check if user has permission")
    public ResponseEntity<Boolean> checkPermission(
            @PathVariable UUID userId,
            @RequestParam String permission) {
        return ResponseEntity.ok(roleService.hasPermission(userId, permission));
    }

    // Permission endpoints
    @GetMapping("/permissions")
    @Operation(summary = "List all permissions")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(roleService.getAllPermissions());
    }

    @PostMapping("/permissions")
    @Operation(summary = "Create a new permission")
    public ResponseEntity<Permission> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        Permission permission = roleService.createPermission(
                request.name(), request.resource(), request.action(), request.description());
        return ResponseEntity.ok(permission);
    }

    public record CreateRoleRequest(
            @NotBlank String name,
            String description,
            Set<UUID> permissionIds
    ) {}

    public record AssignRoleRequest(
            UUID userId,
            UUID roleId,
            UUID assignedBy
    ) {}

    public record CreatePermissionRequest(
            @NotBlank String name,
            @NotBlank String resource,
            @NotBlank String action,
            String description
    ) {}
}
