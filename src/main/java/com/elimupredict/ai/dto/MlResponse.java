package com.elimupredict.ai.dto;

import lombok.Data;

@Data
public class MlResponse {

    private String admissionNumber;
    private Long subjectId;
    private Double riskPercentage;
    private String riskLevel;
    private Integer weaknessGroup;
}
