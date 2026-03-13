package com.elimupredict.ai;

import com.elimupredict.ai.dto.AnalysisResponse;
import com.elimupredict.ai.dto.MlRequest;
import com.elimupredict.ai.dto.MlResponse;
import com.elimupredict.common.enums.Term;
import com.elimupredict.marks.StudentRecord;
import com.elimupredict.marks.StudentRecordRepository;
import com.elimupredict.student.StudentService;
import com.elimupredict.subject.Subject;
import com.elimupredict.subject.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {

    private final StudentRecordRepository recordRepository;
    private final AIAnalysisRepository analysisRepository;
    private final MlService mlService;
    private final GeminiService geminiService;
    private final StudentService studentService;
    private final SubjectService subjectService;

    // ── Analyze one student across all their subjects for a term ──
    public List<AnalysisResponse> analyzeStudent(
            String admissionNumber, Term term, Integer academicYear) {

        // Validate student exists
        studentService.findOrThrow(admissionNumber);

        // Get all marks for this student in this term
        List<StudentRecord> records =
                recordRepository.findByAdmissionNumberAndTerm(admissionNumber, term);

        if (records.isEmpty()) {
            throw new RuntimeException(
                    "No marks found for student " + admissionNumber +
                            " in term " + term);
        }

        // Group records by subjectId
        Map<Long, List<StudentRecord>> bySubject = records.stream()
                .collect(Collectors.groupingBy(StudentRecord::getSubjectId));

        List<AnalysisResponse> results = new ArrayList<>();

        for (Map.Entry<Long, List<StudentRecord>> entry : bySubject.entrySet()) {
            Long subjectId = entry.getKey();
            List<StudentRecord> subjectRecords = entry.getValue();

            Subject subject = subjectService.getById(subjectId);

            AnalysisResponse response = runAnalysis(
                    admissionNumber, subjectId,
                    subject.getSubjectName(),
                    subjectRecords, term, academicYear
            );
            results.add(response);
        }

        return results;
    }

    // ── Analyze all students in a class ──
    public Map<String, List<AnalysisResponse>> analyzeClass(
            String className, Term term, Integer academicYear) {

        List<String> admissionNumbers = studentService
                .getStudentsByClassName(className)
                .stream()
                .map(s -> s.getAdmissionNumber())
                .toList();

        if (admissionNumbers.isEmpty()) {
            throw new RuntimeException("No students found in class: " + className);
        }

        Map<String, List<AnalysisResponse>> classResults = new LinkedHashMap<>();

        for (String admNo : admissionNumbers) {
            try {
                List<AnalysisResponse> studentResults =
                        analyzeStudent(admNo, term, academicYear);
                classResults.put(admNo, studentResults);
            } catch (Exception e) {
                log.warn("Skipping student {} — {}", admNo, e.getMessage());
            }
        }

        log.info("Class analysis complete for {} — {} students analyzed",
                className, classResults.size());

        return classResults;
    }

    // ── Core pipeline for one student + one subject ──
    private AnalysisResponse runAnalysis(
            String admissionNumber, Long subjectId, String subjectName,
            List<StudentRecord> records, Term term, Integer academicYear) {

        // Sort records by exam type order
        records.sort(Comparator.comparing(r -> r.getExamType().ordinal()));

        List<Double> marks = records.stream()
                .map(StudentRecord::getMarksObtained)
                .toList();

        List<String> examTypes = records.stream()
                .map(r -> r.getExamType().name())
                .toList();

        // ── Step 1: Call ML service ──
        MlRequest mlRequest = MlRequest.builder()
                .admissionNumber(admissionNumber)
                .subjectId(subjectId)
                .subjectName(subjectName)
                .marks(marks)
                .examTypes(examTypes)
                .term(term.name())
                .academicYear(academicYear)
                .build();

        MlResponse mlResponse;
        String status = "COMPLETED";

        try {
            mlResponse = mlService.predict(mlRequest);
        } catch (Exception e) {
            log.error("ML prediction failed for {}: {}", admissionNumber, e.getMessage());
            mlResponse = null;
            status = "FAILED";
        }

        // ── Step 2: Call Gemini only if risk >= 40% ──
        String suggestion = null;
        if (mlResponse != null && mlResponse.getRiskPercentage() != null
                && mlResponse.getRiskPercentage() >= 40.0) {
            suggestion = geminiService.generateSuggestion(
                    subjectName, mlResponse.getRiskPercentage(), marks);
        }

        // ── Step 3: Save or update analysis record ──
        AiAnalysis analysis = analysisRepository
                .findByAdmissionNumberAndSubjectIdAndTermAndAcademicYear(
                        admissionNumber, subjectId, term, academicYear)
                .orElse(AiAnalysis.builder()
                        .admissionNumber(admissionNumber)
                        .subjectId(subjectId)
                        .term(term)
                        .academicYear(academicYear)
                        .build());

        if (mlResponse != null) {
            analysis.setRiskPercentage(mlResponse.getRiskPercentage());
            analysis.setRiskLevel(mlResponse.getRiskLevel());
            analysis.setWeaknessGroup(mlResponse.getWeaknessGroup());
        }
        analysis.setSuggestion(suggestion);
        analysis.setAnalysisStatus(status);

        analysisRepository.save(analysis);

        // ── Step 4: Return response ──
        return AnalysisResponse.builder()
                .admissionNumber(admissionNumber)
                .subjectName(subjectName)
                .riskPercentage(mlResponse != null ? mlResponse.getRiskPercentage() : null)
                .riskLevel(mlResponse != null ? mlResponse.getRiskLevel() : null)
                .suggestion(suggestion)
                .analysisStatus(status)
                .term(term.name())
                .academicYear(academicYear)
                .build();
    }

    // ── Retrieve stored results ──
    public List<AnalysisResponse> getStudentResults(
            String admissionNumber, Term term) {

        return analysisRepository
                .findByAdmissionNumberAndTerm(admissionNumber, term)
                .stream()
                .map(a -> AnalysisResponse.builder()
                        .admissionNumber(a.getAdmissionNumber())
                        .riskPercentage(a.getRiskPercentage())
                        .riskLevel(a.getRiskLevel())
                        .suggestion(a.getSuggestion())
                        .analysisStatus(a.getAnalysisStatus())
                        .term(a.getTerm().name())
                        .academicYear(a.getAcademicYear())
                        .build())
                .toList();
    }

    public List<AiAnalysis> getAllStudentResults(String admissionNumber) {
        return analysisRepository.findByAdmissionNumber(admissionNumber);
    }
}