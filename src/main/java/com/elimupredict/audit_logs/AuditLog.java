package com.elimupredict.audit_logs;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String userRole;
    private String action;
    private String description;
    private String ipAddress;
    @Column(nullable = false)
    private LocalDateTime timestamp;
}