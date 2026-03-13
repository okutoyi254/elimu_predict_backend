package com.elimupredict.ai;

import com.elimupredict.common.enums.Term;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analysis",
uniqueConstraints = @UniqueConstraint(
        columnNames = {"admission_number","subject_id","term","academic_year"}
))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_number", nullable = false)
    private String admissionNumber;

    @Column(name ="subject_id",nullable = false)
    private Long subjectId;

    private Double riskPercentage;

    private String riskLevel;

    private Integer weaknessGroup;

    @Column( columnDefinition = "TEXT")
    private String suggestion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Term term;

    @Column(nullable = false)
    private Integer academicYear;

    @Column(nullable = false)
    private String analysisStatus;      // PENDING, COMPLETED, FAILED

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}
