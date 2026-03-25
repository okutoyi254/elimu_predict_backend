package com.elimupredict.auth.dto;

import com.elimupredict.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String username;
    private Role role;
    private String message;
}
