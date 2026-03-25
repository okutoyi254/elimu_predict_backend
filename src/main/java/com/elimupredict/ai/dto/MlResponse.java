package com.elimupredict.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class MlResponse {

    private Logger log = LoggerFactory.getLogger(MlResponse.class);

    private String admissionNumber;
    private Long subjectId;
    private Double riskPercentage;
    private String riskLevel;
    private Integer weaknessGroup;
    private Double confidence;
    private Integer trend;

    @JsonProperty("registration_number")
    public void setRegistrationNumber(String v){
        this.admissionNumber=v;
    }

    @JsonProperty("subject_id")
    public void setSubjectIdFromFlask(Integer v){
        this.subjectId = v ==null? null: v.longValue();
    }

    @JsonProperty("risk")
    public void setRiskFromLabel(String label) {
        if (label == null) return;

        switch (label.trim()) {
            case "Doing Well" -> {
                this.riskPercentage = 15.0;
                this.riskLevel = "LOW";
                this.weaknessGroup = 0;
            }
            case "Stable" -> {
                this.riskPercentage = 45.0;
                this.riskLevel = "MEDIUM";
                this.weaknessGroup = 1;
            }
            case "Risky" -> {
                this.riskPercentage = 68.0;
                this.riskLevel = "MEDIUM";
                this.weaknessGroup = 1;
            }
            case "High Risk" -> {
                this.riskPercentage = 85.0;
                this.riskLevel = "HIGH";
                this.weaknessGroup = 2;
            }
            default -> {
                log.warn("[ML RESPONSE] Unknown label '{}' — defaulting to MEDIUM", label);
                this.riskPercentage = 50.0;
                this.riskLevel = "MEDIUM";
                this.weaknessGroup = 1;
            }
        }
        this.confidence = 0.87;
    }
    public void applyTrend() {
        if (trend == null || riskPercentage == null) return;

        double adjusted = riskPercentage;
        if (trend <= -20) adjusted = Math.min(100.0, adjusted + 15);
        else if (trend <= -10) adjusted = Math.min(100.0, adjusted + 8);
        else if (trend >= 20) adjusted = Math.max(0.0, adjusted - 15);
        else if (trend >= 10) adjusted = Math.max(0.0, adjusted - 8);

        this.riskPercentage = Math.round(adjusted * 10.0) / 10.0;

        // Re-evaluate level after trend
        if (this.riskPercentage >= 70) {
            this.riskLevel = "HIGH";
            this.weaknessGroup = 2;
        } else if (this.riskPercentage >= 40) {
            this.riskLevel = "MEDIUM";
            this.weaknessGroup = 1;
        } else {
            this.riskLevel = "LOW";
            this.weaknessGroup = 0;
        }
    }


}
