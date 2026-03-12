package com.elimupredict.subject.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubjectRequest {
    @NotBlank private String subjectCode;
    @NotBlank private String subjectName;
    private Long teacherId;
    @NotBlank private String className;
}
