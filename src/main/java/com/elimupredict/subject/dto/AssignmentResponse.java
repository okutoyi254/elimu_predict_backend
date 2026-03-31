package com.elimupredict.subject.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AssignmentResponse {
    private Long id;
    private Long teacherId;
    private String teacherName;
    private String teacherUserId;
    private Long subjectId;
    private String subjectName;
    private String className;
    private Boolean isActive;
    private LocalDateTime assignedAt;
    private String assignedBy;
}