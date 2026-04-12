package com.elimupredict.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProgressTimeLineDTO {

    private String admissionNumber;
    private String fullName;
    private List<TermSnapshotDTO> timeLine;
    private String overallTrajectory;
    private String comment;

    @Data @Builder
    public static class TermSnapshotDTO {
        private String term;
        private Integer academicYear;
        private Double averageRiskScore;
        private String overallRiskLeve;
        private List<SubjectSnapshotDTO> subjectPerformances;
    }

    @Data @Builder
    public static  class SubjectSnapshotDTO{

        private String subjectName;
        private Double riskPercentage;
        private String riskLevel;
        private Integer trend;
    }

}
