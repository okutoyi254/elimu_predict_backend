package com.elimupredict.subject.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AssignmentResponse {

    private String teacherName;
    private String subjectName;
    private String className;
    private Boolean isActive;
    private LocalDateTime assignedAt;
}