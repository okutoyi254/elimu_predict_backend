package com.elimupredict.marks;

import com.elimupredict.common.enums.ExamType;
import com.elimupredict.common.enums.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRecordRepository extends JpaRepository<StudentRecord,Long> {

    List<StudentRecord> findByAdmissionNumber(String admissionNumber);
    List<StudentRecord>findByAdmissionNumberAndTerm(String admissionNumber, Term term);
    List<StudentRecord>findByAdmissionNumberAndSubjectId(String admissionNumber,Long subjectId);
    List<StudentRecord>findBySubjectIdAndTermAndAcademicYear(Long subjectId,Term term, Integer academicYear);
    Optional<StudentRecord> findByAdmissionNumberAndSubjectIdAndExamTypeAndTermAndAcademicYear(
            String admissionNumber, Long subjectId,
            ExamType examType, Term term, Integer academicYear);

    boolean existsByAdmissionNumberAndSubjectIdAndExamTypeAndTermAndAcademicYear(
            String admissionNumber, Long subjectId,
            ExamType examType, Term term, Integer academicYear);

    // Get all marks for a whole class via subject
    @Query("SELECT sr FROM StudentRecord sr WHERE sr.subjectId = :subjectId " +
            "AND sr.term = :term AND sr.academicYear = :year")
    List<StudentRecord> findClassRecords(Long subjectId, Term term, Integer year);

    // All records for a student across all subjects
    List<StudentRecord> findByAdmissionNumberAndAcademicYear(
            String admissionNumber, Integer academicYear);
    }

