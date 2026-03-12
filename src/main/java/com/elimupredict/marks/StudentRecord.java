package com.elimupredict.marks;

import com.elimupredict.common.enums.ExamType;
import com.elimupredict.common.enums.Term;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name ="student_records",
uniqueConstraints = @UniqueConstraint(
        columnNames = {"admission_number","subject_id","exam_type","term","academic_year"}
))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_id",nullable = false)
    private Long subjectId;

    @Column(name = "admission_number", nullable = false)
    private String admissionNumber;

    @Column(nullable = false)
    private Double marksObtained;

    @Column(nullable = false)
    private Double totalMarks =100.0;

    @Enumerated(EnumType.STRING)
    @Column(name="exam_type",nullable = false)
    private ExamType examType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Term term;

    @Column(nullable = false)
    private Integer academicYear;

    @Column(nullable = false,updatable = false)
    private LocalDateTime uploadedAt;

    @Column(nullable = false)
    private Long uploadedBy;

    @PrePersist
    protected void onCreate(){
        uploadedAt = LocalDateTime.now();
    }
}
