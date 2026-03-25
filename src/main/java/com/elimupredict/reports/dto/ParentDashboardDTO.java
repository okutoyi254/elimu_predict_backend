package com.elimupredict.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ParentDashboardDTO {

    private String parentId;
    private Integer totalChildren;

    private List<ChildSummaryDTO> children;



    @Data
    @Builder
    public static class ChildSummaryDTO{
        private String admissionNumber;
        private String fullName;
        private String className;
        private Integer enrollmentYear;
        private String overallRiskLevel;
        private String overallMessage;
        private Double averageRiskScore;
        private Integer trend;

        private List<StudentReportDTO.SubjectRiskDTO> subjectBreakdown;

    }

}
