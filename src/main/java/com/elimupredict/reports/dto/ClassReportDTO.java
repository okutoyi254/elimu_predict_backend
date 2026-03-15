package com.elimupredict.reports.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ClassReportDTO {
    private String className;
    private Integer totalStudents;
    private Integer analyzedStudents;

    // Class-level subject weakness percentages
    // e.g. "70% of students are weak in Math"
    private List<SubjectWeaknessDTO> subjectWeaknesses;

    // Individual student breakdown
    private List<StudentSummaryDTO> studentSummaries;

    // Risk distribution
    private Long highRiskCount;
    private Long mediumRiskCount;
    private Long lowRiskCount;

    @Data
    @Builder
    public static class SubjectWeaknessDTO {
        private String subjectName;
        private Double weaknessPercentage;  // % of students weak in this subject
        private Integer affectedStudents;
    }

    @Data
    @Builder
    public static class StudentSummaryDTO {
        private String admissionNumber;
        private String fullName;
        private String overallRiskLevel;
        private Double averageRiskScore;
    }
}