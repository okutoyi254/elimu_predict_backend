package com.elimupredict.student.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StudentResponse {

    private String admissionNumber;
    private String fullName;
    private String className;
    private Long parentId;
    private Integer enrollmentYear;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
