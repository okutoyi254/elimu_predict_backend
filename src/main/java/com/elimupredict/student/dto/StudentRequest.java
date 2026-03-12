package com.elimupredict.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentRequest {

    @NotBlank(message = "Admission number is required")
    private String admissionNumber;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Class name is required")
    private String className;

    private Long parentId;

    @NotNull(message = "Enrollment year is required")
    private Integer enrollmentYear;
}
