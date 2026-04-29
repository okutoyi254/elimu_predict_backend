package com.elimupredict.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
@Builder
@Data
public class ParentStudentProfileDTO {

    private String admissionNumber;
    private String fullName;
    private String className;
    private String term;
    private Integer academicYear;

    private PerformanceTabDTO performance;

    private AnalysisTabDTO analysis;

    private AssessmentTabDTO assessmentBreakDown;

    @Data
    @Builder
    public static class PerformanceTabDTO {

        private Double totalMarksObtained;
        private Double totalMarksAvailable;
        private String overallGrade;
        private Integer classPosition;
        private Integer totalStudents;
        private String performanceBand;

        private String bestSubject;
        private Double bestSubjectMean;
        private String worstSubject;
        private Double worstSubjectMean;

        @Data
        @Builder
        public static class SubjectPerformanceRowDTO {
            private String subjectName;
            private Double mean;
            private Double total;
            private Double percentage;
            private String grade;
            private Integer positionInClass;
            private String performanceVsClass;
        }
    }

    @Data
    @Builder
    public static class AnalysisTabDTO {
        private Double averageRiskScore;
        private String overallRiskLevel;
        private String trajectoryLabel;         // IMPROVING, DECLINING, STABLE
        private String trajectoryMessage;
        private Integer overallTrend;

        // Previous term comparison
        private Double currentTermMean;
        private Double previousTermMean;
        private Double meanChange;
        private String previousTerm;
        private Integer previousYear;
        private Boolean hasComparison;

        private List<SubjectRiskSummaryDTO> subjectRiskSummaries;

        private List<String> highRiskSubjects;
        private List<String> mediumRiskSubjects;
        private List<String> lowRiskSubjects;

        private String parentSummary;

        @Data @Builder
        public static class SubjectRiskSummaryDTO {
            private String subjectName;
            private Double riskPercentage;
            private String riskLevel;
            private Integer trend;
            private String trendDirection;
            private String improvement;         // vs last term
            private Double previousTermMean;
            private Double currentTermMean;
            private Double markChange;
        }
    }

    @Data @Builder
    public static class AssessmentTabDTO {

        private Double cat1Average;
        private Double cat2Average;
        private Double cat3Average;
        private Double exam1Average;
        private Double exam2Average;

        private List<GraphPointDTO> graphData;

        private List<SubjectAssessmentDTO> subjectAssessments;

        @Data @Builder
        public static class GraphPointDTO {
            private String label;
            private Double average;
            private Double classAverage;
        }

        @Data @Builder
        public static class SubjectAssessmentDTO {
            private String subjectName;
            private Double cat1;
            private Double cat2;
            private Double cat3;
            private Double exam1;
            private Double exam2;
            private Double mean;
            private String trend;
        }
        }
}
