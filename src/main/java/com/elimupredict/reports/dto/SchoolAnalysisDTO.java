package com.elimupredict.reports.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SchoolAnalysisDTO {

    private String term;
    private Integer academicYear;
    private String generatedAt;

    private String overallAnalysis;
    private String principalRecommendation;

    private List<FocusAreaDTO> areasNeedingAttention;
    private List<FocusAreaDTO> areasPerformingWell;

    private List<ActionItemDTO> actionPlan;

    @Data @Builder
    public static class FocusAreaDTO {
        private String area;
        private String reason;
        private String priority;        // HIGH, MEDIUM, LOW
        private String recommendation;
    }

    @Data @Builder
    public static class ActionItemDTO {
        private Integer priority;
        private String action;
        private String targetClass;
        private String targetSubject;
        private String expectedOutcome;
    }
}
