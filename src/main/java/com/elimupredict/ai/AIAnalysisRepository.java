package com.elimupredict.ai;

import com.elimupredict.common.enums.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AIAnalysisRepository extends JpaRepository<AiAnalysis,Long> {

    List<AiAnalysis> findByAdmissionNumber(String admissionNumber);
    List<AiAnalysis> findByAdmissionNumberAndTerm(String admissionNumber, Term term);

    Optional<AiAnalysis> findByAdmissionNumberAndSubjectIdAndTermAndAcademicYear(
            String admissionNumber, Long subjectId,Term term, Integer academicYear
    );

    List<AiAnalysis> findByAdmissionNumberAndTermAndAcademicYear(String admissionNumber, Term term, Integer academicYear);

    @Query("SELECT a FROM AiAnalysis a WHERE  a.subjectId = :subjectId " +
            "AND a.term = :term AND a.academicYear = :year " +
            "AND a.riskLevel = 'HIGH'")
    List<AiAnalysis> findHighRiskBySubject(Long subjectId,Term term,Integer year);

    @Query("SELECT a FROM AiAnalysis a WHERE a.admissionNumber IN :admissionNumbers " +
            "AND a.term = :term AND a.academicYear = :year")
    List<AiAnalysis> findByStudentsAndTerm(
            List<String> admissionNumbers, Term term, Integer year);

    List<AiAnalysis> findByAdmissionNumberAndAcademicYear(
            String admissionNumber, Integer academicYear);
}
