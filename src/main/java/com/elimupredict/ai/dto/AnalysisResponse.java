package com.elimupredict.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalysisResponse {

    private String admissionNumber;
    private String subjectName;
    private Double riskPercentage;
    private String riskLevel;
    private String suggestion;
    private  String analysisStatus;
    private String term;
    private Integer academicYear;


}
