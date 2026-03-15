package com.elimupredict.reports.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StudentReportDTO {
    private String admissionNumber;
    private String fullName;
    private String className;

    private List<SubjectRiskDTO> subjectRisks;

    private String overallRiskLevel;    // worst risk level across all subjects
    private Double averageRiskScore;

    @Data
    @Builder
    public static class SubjectRiskDTO {
        private String subjectName;
        private Double riskPercentage;
        private String riskLevel;
        private String suggestion;
        private List<Double> marks;
    }
}