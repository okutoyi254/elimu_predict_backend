package com.elimupredict.auth.security;

import com.elimupredict.auth.dto.AuthResponse;
import com.elimupredict.auth.dto.LoginRequest;
import com.elimupredict.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal String registeredBy) {
        return ResponseEntity.ok(authService.register(request, registeredBy));
    }

    @GetMapping("/me")
    public ResponseEntity<String> me(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok("Logged in as: " + userId);
    }
}
