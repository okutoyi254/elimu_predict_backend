package com.elimupredict.reports.dto;


import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SuggestionTabDTO {

    private String admissionNumber;
    private String fullName;
    private String term;
    private Integer academicYear;
    private String overallMessage;

    private List<SubjectSuggestionDTO> subjectSuggestions;

    private Integer subjectsNeedingAttention;
    private Integer subjectsPerformingWell;

    @Data @Builder
    public static class SubjectSuggestionDTO {
        private String subjectName;
        private String riskLevel;
        private Double riskPercentage;
        private Double currentMean;
        private String suggestion;
        private Boolean freshlyGenerated;
        private List<String> actionPoints;
    }
}
