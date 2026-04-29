package com.elimupredict.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeacherProfileDTO {

    private String teacherName;
    private Integer totalAssignments;
    private Integer totalClassesTaught;
    private Integer totalStudentsTaught;

    private List<SubjectCardDTO> subjectCards;

    @Data
    @Builder
    public static class SubjectCardDTO {
        private String subjectName;
        private String subjectCode;
        private String className;
        private Integer totalStudents;
        private Integer analyzedStudents;

        private Double classAverageMark;
        private Double classMeanRisk;
        private String subjectStatus;
        private Integer highRiskCount;
        private Integer lowRiskCount;

        private Double previousTermAvg;
        private Double avgChange;
        private String trend;              // IMPROVING, DECLINING, STABLE
        private String trendMessage;

        private List<AssessmentAvgDTO> assessmentGraph;

        private List<AtRiskStudentDTO> atRiskStudents;

        private List<TopStudentDTO> topStudents;

        @Data
        @Builder
        public static class AssessmentAvgDTO {
            private String label;          // "CAT 1", "CAT 2" etc
            private Double classAverage;   // avg mark for whole class
            private Double highRiskAvg;    // avg for high risk students
            private Double lowRiskAvg;
        }

        @Data
        @Builder
        public static class AtRiskStudentDTO {
            private String admissionNumber;
            private String fullName;
            private Double subjectMean;
            private Double riskPercentage;
            private String riskLevel;
            private String trendDirection; // IMPROVING, DECLINING, STABLE
            private Integer trend;
        }

        @Data
        @Builder
        public static class TopStudentDTO {
            private String admissionNumber;
            private String fullName;
            private Double subjectMean;
            private Double riskPercentage;
            private String trendDirection;
        }
    }
}