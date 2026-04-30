package com.elimupredict.subject.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TeacherClassSubjectDTO {
    private String teacherUsername;
    private String teacherName;

    private List<String> availableClasses;

    private List<ClassSubjectDTO> classSubjects;

    @Data
    @Builder
    public static class ClassSubjectDTO {
        private String className;
        private List<SubjectOptionDTO> subjects;
    }

    @Data
    @Builder
    public static class SubjectOptionDTO {
        private Long subjectId;
        private String subjectName;
        private String subjectCode;
    }
}