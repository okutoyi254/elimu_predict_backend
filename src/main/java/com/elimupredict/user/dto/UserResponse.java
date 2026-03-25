package com.elimupredict.user.dto;

import com.elimupredict.common.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String fullName;
    private String userId;
    private Role role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String createdBy;
}
