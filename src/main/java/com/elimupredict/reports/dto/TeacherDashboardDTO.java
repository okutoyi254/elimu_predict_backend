package com.elimupredict.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeacherDashboardDTO {

    private String teacherId;
    private String teacherName;

    private List<AtRiskStudentDTO> atRiskStudents;
    private Long highRiskCount;
    private Long mediumRiskCount;

//    Class weakness overview
    private List<ClassReportDTO.SubjectWeaknessDTO> classWeakness;

    @Data
    @Builder
    public static class AtRiskStudentDTO{

        private String admissionNumber;
        private String fullName;
        private String riskLevel;
        private Double riskPercentage;
        private String weaknessSubject;
        private String suggestion;
    }
}
