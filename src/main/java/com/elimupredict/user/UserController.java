package com.elimupredict.user;

import com.elimupredict.common.ApiVersion;
import com.elimupredict.common.enums.Role;
import com.elimupredict.user.dto.RegisterRequest;
import com.elimupredict.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1+"/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;


    // ADMIN — can register anyone
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_HANDLER')")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal String registeredBy) {

        log.info("REQUEST: {}", request);
        log.info("FULL NAME: {}", request.getFullName());
        log.info("USERNAME: {}", request.getUsername());
        // IT_HANDLER cannot register ADMIN
        if (request.getRole() == Role.ADMIN) {
            throw new RuntimeException(
                    "IT_HANDLER cannot register ADMIN accounts. " +
                            "Contact system administrator.");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.registerUser(request, registeredBy));
    }

    // ADMIN-only registration — can register anyone including other admins
    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> adminRegister(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal String registeredBy) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.registerUser(request, registeredBy));
    }


    @PutMapping("/{parentId}/link-student/{admissionNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_HANDLER')")
    public ResponseEntity<UserResponse> linkParentToStudent(
            @PathVariable Long parentId,
            @PathVariable String admissionNumber) {
        return ResponseEntity.ok( userService.linkParentToStudent(parentId,admissionNumber)
        );
    }



    @PutMapping("/{id}/assign-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable Long id,
            @RequestParam Role role) {
        return ResponseEntity.ok(userService.assignRole(id, role));
    }

    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> revokeAccess(@PathVariable Long id) {
        return ResponseEntity.ok(userService.revokeAccess(id));
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> restoreAccess(@PathVariable Long id) {
        return ResponseEntity.ok(userService.restoreAccess(id));
    }


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_HANDLER')")
    public ResponseEntity<List<UserResponse>> getByRole(@PathVariable Role role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_HANDLER')")
    public ResponseEntity<UserResponse> getOne(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getByUserId(userId));
    }
}
