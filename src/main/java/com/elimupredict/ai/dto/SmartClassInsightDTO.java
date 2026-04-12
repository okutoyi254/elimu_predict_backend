package com.elimupredict.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SmartClassInsightDTO {

    private String className;
    private Integer totalStudents;
    private String term;
    private Integer academicYear;
    private String criticalSubject;
    private Integer totalWeakStudents;

    // Per-subject breakdown
    private List<SubjectInsightDTO> subjectInsights;

    // Students at risk in multiple subjects
    private List<MultiSubjectRiskDTO> multiSubjectRiskStudents;

    // Mixed performers — strong in some, weak in others
    private List<MixedPerformerDTO> mixedPerformers;


    @Data
    @Builder
    public static class HiddenWeakStudentDTO {

       private String admissionNumber;
       private String fullName;
       private Double riskPercentage;
       private String riskLevel;
       private Double deviationFromTheClassAvg;
       private String suggestion;
       private String urgency;



    }

    @Data @Builder
    public static class  SubjectInsightDTO{

        private Long subjectId;
        private String subjectName;
        private Double classAverageRisk;
        private String subjectStatus;
        private String actionRequired;
        private Integer highRiskCount;
        private Integer mediumRiskCount;
        private Integer lowRiskCount;
        private Integer hiddenStrugglerCount;
        private Double standardDeviation;
        private List<HiddenWeakStudentDTO> hiddenStrugglers;
        private List<String> performanceOutliersHigh;
        private List<String> performanceOutliersLow;
    }

    @Data @Builder
    public static class MultiSubjectRiskDTO{

        private String admissionNumber;
        private String fullName;
        private List<String> highRiskSubjects;
        private List<String> mediumRiskSubjects;
        private Integer totalAtRiskSubjects;
        private String overallUrgency;
    }

    @Data @Builder
    public static class MixedPerformerDTO{

        private String admissionNumber;
        private String fullName;
        private List<String> strongSubjects;
        private List<String> weakSubjects;
        private Boolean isMixedPerformer;
        private String insight;
    }
}