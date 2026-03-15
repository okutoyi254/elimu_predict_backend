package com.elimupredict.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SchoolOverviewDTO {

    private Integer totalStudents;
    private Integer totalClasses;
    private Long totalHighRisk;
    private Long totalMediumRisk;
    private Long totalLowRisk;
    private List<ClassSummaryDTO> classSummaries;

    @Data
    @Builder
    public static class ClassSummaryDTO{
        private String className;
        private Integer totalStudents;
        private Long highRiskCount;
        private Double averageRiskScore;
        private String mostWeakSubject;
    }
}
