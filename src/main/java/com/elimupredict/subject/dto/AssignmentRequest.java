package com.elimupredict.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignmentRequest {

    @NotNull(message = "Teacher username is required")
    private String username;

    @NotNull(message = "Subject code is required")
    private String subjectCode;

    @NotBlank(message = "Class name is required")
    private String className;
}
