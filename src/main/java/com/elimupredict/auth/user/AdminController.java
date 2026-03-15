package com.elimupredict.auth.user;

import com.elimupredict.audit_logs.AuditLog;
import com.elimupredict.audit_logs.AuditLogRepository;
import com.elimupredict.common.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> getAllLogs() {
        return ResponseEntity.ok(
                auditLogRepository.findAllByOrderByTimestampDesc());
    }

    @GetMapping("/logs/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> getUserLogs(
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                auditLogRepository.findByUserId(userId));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PutMapping("/users/{id}/assign-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> assignRole(
            @PathVariable Long id,
            @RequestParam Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(role);
        userRepository.save(user);
        return ResponseEntity.ok("Role assigned: " + role);
    }

    @PutMapping("/users/{id}/revoke-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> revokeAccess(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
        return ResponseEntity.ok("Access revoked for user: " + user.getFullName());
    }
}

