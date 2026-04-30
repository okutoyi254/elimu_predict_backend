package com.elimupredict.auth.security;

import com.elimupredict.audit_logs.AuditLog;
import com.elimupredict.audit_logs.AuditLogRepository;
import com.elimupredict.auth.dto.AuthResponse;
import com.elimupredict.auth.dto.LoginRequest;
import com.elimupredict.auth.dto.RegisterRequest;
import com.elimupredict.user.User;
import com.elimupredict.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AuditLogRepository auditLogRepository;


    public AuthResponse login(LoginRequest request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(()->new RuntimeException("User not found"));

        String token =jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        auditLogRepository.save(AuditLog.builder()
                .userId(user.getId())
                .userRole(user.getRole().name())
                .action("LOGIN")
                .description(user.getUsername() + " logged in")
                .timestamp(LocalDateTime.now())
                .build());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .message("Login successful")
                .build();

    }

    public AuthResponse register(RegisterRequest request, String registerBy){
        if(userRepository.existsByUsername(request.getUsername())){
            throw new RuntimeException("Username "+request.getUsername() +" already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .isActive(true)
                .createdBy(registerBy)
                .fullName(request.getFullName())
                .role(request.getRole())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        return AuthResponse.builder()
                .message("User registered successfully")
                .role(user.getRole())
                .username(user.getUsername())
                .build();
    }




}
