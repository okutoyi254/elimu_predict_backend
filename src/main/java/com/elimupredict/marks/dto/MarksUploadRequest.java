package com.elimupredict.marks.dto;

import com.elimupredict.common.enums.ExamType;
import com.elimupredict.common.enums.Term;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MarksUploadRequest {

    @NotBlank(message = "Admission number is required")
    private String admissionNumber;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull
    @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
    private Double marksObtained;

    @NotNull(message = "Exam type is required")
    private ExamType examType;          // CAT_1, CAT_2, CAT_3, EXAM_1, EXAM_2

    @NotNull(message = "Term is required")
    private Term term;

    @NotNull(message = "Academic year is required")
    private Integer academicYear;
}
