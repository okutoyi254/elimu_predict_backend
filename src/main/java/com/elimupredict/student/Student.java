package com.elimupredict.student;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,nullable = false)
    private String admissionNumber;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String className;

    private Long parentId;

    @Column(nullable = false)
    private  Integer enrollmentYear;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
