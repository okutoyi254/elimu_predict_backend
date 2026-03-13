package com.elimupredict.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data

@Builder
public class MlRequest {

    private String admissionNumber;
    private Long subjectId;
    private String subjectName;
    private List<Double> marks;
    private List<String> examTypes;
    private String term;
    private Integer academicYear;
}
