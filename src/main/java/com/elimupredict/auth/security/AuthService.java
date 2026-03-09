package com.elimupredict.auth.security;

import com.elimupredict.auth.dto.AuthResponse;
import com.elimupredict.auth.dto.LoginRequest;
import com.elimupredict.auth.dto.RegisterRequest;
import com.elimupredict.auth.user.User;
import com.elimupredict.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(LoginRequest request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUserName(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(()->new RuntimeException("User not found"));

        String token =jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .userName(user.getUsername())
                .role(user.getRole())
                .message("Login successful")
                .build();

    }

    public AuthResponse register(RegisterRequest request, String registerBy){
        if(userRepository.existsByUserName(request.getUserName())){
            throw new RuntimeException("Username "+request.getUserName() +" already exists");
        }

        User user = User.builder()
                .userName(request.getUserName())
                .isActive(true)
                .createdBy(registerBy)
                .password(passwordEncoder.encode(request.getFullName()))
                .role(request.getRole())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        return AuthResponse.builder()
                .message("User registered successfully")
                .role(user.getRole())
                .userName(user.getUsername())
                .build();
    }
}
