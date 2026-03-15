package com.elimupredict.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ParentDashboardDTO {

    private String parentId;
    private String childName;
    private String admissionNumber;
    private String className;

    private String overallRiskLevel;
    private String overallMessage;

    private List<StudentReportDTO.SubjectRiskDTO> subjectBreakdown;


}
