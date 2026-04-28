package com.elimupredict.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PrincipalDashboardDTO {

    private String term;
    private Integer academicYear;

    private Integer totalStudents;
    private Integer totalClasses;
    private Integer totalSubjects;
    private Double schoolMeanAverage;
    private String schoolGrade;
    private Double previousTermMean;
    private Double meanChange;
    private String schoolTrend;

    private List<ClassGraphDTO> classGraphs;

    private List<ClassRankDTO> classRankings;

    private List<SubjectRankDTO> subjectRankings;

    private MostImprovedDTO mostImprovedClasses;

    @Data @Builder
    public static class ClassGraphDTO {

        private String className;
        private Integer totalStudents;
        private Double classMeanAverage;
        private String classGrade;
        private String classStatus;
        private Integer classRank;

        private List<GraphPointDTO> graphPoints;

        private Long highRiskCount;
        private Long mediumRiskCount;
        private Long lowRiskCount;

        private Double previousTerMean;
        private Double meanChange;
        private String trend;

    }

        @Data @Builder
        public static class GraphPointDTO {
            private String label;
            private Double classAverage;
            private Double schoolAverage;
        }

        @Data @Builder
        public static class ClassRankDTO {
            private Integer rank;
            private String className;
            private Double meanAverage;
            private String grade;
            private String trend;
            private Double meanChange;
            private Long highRiskCount;
            private Integer totalStudents;
            private String bestSubject;
            private String worstSubject;
        }

        @Data @Builder
        public static class SubjectRankDTO {
            private Integer rank;
            private String subjectName;
            private String subjectCode;
            private Double schoolWideMean;
            private String grade;
            private String trend;
            private Double meanChange;
            private Long highRiskCount;
            private Long totalStudents;
            private String status;          // CRITICAL, CONCERNING, MODERATE, HEALTHY
        }

        @Data @Builder
        public static class MostImprovedDTO {
            private String className;
            private Double previousMean;
            private Double currentMean;
            private Double improvement;
            private String improvementPercentage;
            private String message;
        }

    }

