package com.elimupredict.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SeniorDashboardDTO {

    private String seniorTeacherId;

    private List<ResourceAllocationDTO> resourceRecommendations;

    private List<ClassReportDTO.SubjectWeaknessDTO> overallWeakness;

    private Long totalRiskStudents;

    @Data
    @Builder
    private static class ResourceAllocationDTO{
        private String subjectName;
        private Integer affectedStudents;
        private String recommendation;
        private String priority;
    }
}
