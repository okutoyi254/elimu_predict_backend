package com.elimupredict.reports;

import com.elimupredict.ai.AiAnalysis;
import com.elimupredict.ai.AIAnalysisRepository;

import com.elimupredict.common.enums.Term;
import com.elimupredict.marks.StudentRecord;
import com.elimupredict.marks.StudentRecordRepository;
import com.elimupredict.reports.dto.*;
import com.elimupredict.student.Student;
import com.elimupredict.student.StudentService;
import com.elimupredict.subject.Subject;
import com.elimupredict.subject.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AIAnalysisRepository analysisRepository;
    private final StudentService studentService;
    private final SubjectService subjectService;
    private final StudentRecordRepository recordRepository;

    // ── Individual student report ──
    public StudentReportDTO getStudentReport(
            String admissionNumber, Term term, Integer academicYear) {

        Student student = studentService.findOrThrow(admissionNumber);

        List<AiAnalysis> analyses = analysisRepository
                .findByAdmissionNumberAndTermAndAcademicYear(admissionNumber, term, academicYear);

        if (analyses.isEmpty()) {
            throw new RuntimeException(
                    "No analysis found for student " + admissionNumber +
                            ". Run analysis first via POST /api/ai/analyze/student/" + admissionNumber);
        }

        List<StudentReportDTO.SubjectRiskDTO> subjectRisks = analyses.stream()
                .map(a -> {
                    Subject subject = subjectService.getById(a.getSubjectId());

                    List<Double> marks = recordRepository
                            .findByAdmissionNumberAndSubjectId(
                                    admissionNumber, a.getSubjectId())
                            .stream()
                            .sorted(Comparator.comparing(
                                    r -> r.getExamType().ordinal()))
                            .map(StudentRecord::getMarksObtained)
                            .toList();

                    return StudentReportDTO.SubjectRiskDTO.builder()
                            .subjectName(subject.getSubjectName())
                            .riskPercentage(a.getRiskPercentage())
                            .riskLevel(a.getRiskLevel())
                            .suggestion(a.getSuggestion())
                            .marks(marks)
                            .build();
                })
                .toList();

        // Overall risk = highest risk level across subjects
        String overallRisk = determineOverallRisk(analyses);

        double avgRisk = analyses.stream()
                .filter(a -> a.getRiskPercentage() != null)
                .mapToDouble(AiAnalysis::getRiskPercentage)
                .average()
                .orElse(0.0);

        return StudentReportDTO.builder()
                .admissionNumber(admissionNumber)
                .fullName(student.getFullName())
                .className(student.getClassName())
                .subjectRisks(subjectRisks)
                .overallRiskLevel(overallRisk)
                .averageRiskScore(Math.round(avgRisk * 10.0) / 10.0)
                .build();
    }

    // ── Class report ──
    public ClassReportDTO getClassReport(
            String className, Term term, Integer academicYear) {

        List<Student> students = studentService.getStudentsByClassName(className)
                .stream()
                .map(s -> studentService.findOrThrow(s.getAdmissionNumber()))
                .toList();

        if (students.isEmpty()) {
            throw new RuntimeException("No students found in class: " + className);
        }

        List<String> admissionNumbers = students.stream()
                .map(Student::getAdmissionNumber)
                .toList();

        List<AiAnalysis> allAnalyses = analysisRepository
                .findByStudentsAndTerm(admissionNumbers, term, academicYear);

        // ── Subject weakness percentages ──
        Map<Long, List<AiAnalysis>> bySubject = allAnalyses.stream()
                .collect(Collectors.groupingBy(AiAnalysis::getSubjectId));

        List<ClassReportDTO.SubjectWeaknessDTO> subjectWeaknesses =
                new ArrayList<>();

        for (Map.Entry<Long, List<AiAnalysis>> entry : bySubject.entrySet()) {
            Subject subject = subjectService.getById(entry.getKey());
            List<AiAnalysis> subjectAnalyses = entry.getValue();

            long weakCount = subjectAnalyses.stream()
                    .filter(a -> "MEDIUM".equals(a.getRiskLevel())
                            || "HIGH".equals(a.getRiskLevel()))
                    .count();

            double weaknessPercent = students.isEmpty() ? 0 :
                    (weakCount * 100.0) / students.size();

            subjectWeaknesses.add(ClassReportDTO.SubjectWeaknessDTO.builder()
                    .subjectName(subject.getSubjectName())
                    .weaknessPercentage(Math.round(weaknessPercent * 10.0) / 10.0)
                    .affectedStudents((int) weakCount)
                    .build());
        }

        // Sort by weakness % descending
        subjectWeaknesses.sort(
                Comparator.comparingDouble(
                                ClassReportDTO.SubjectWeaknessDTO::getWeaknessPercentage)
                        .reversed());

        // ── Student summaries ──
        List<ClassReportDTO.StudentSummaryDTO> studentSummaries =
                students.stream().map(student -> {
                    List<AiAnalysis> studentAnalyses = allAnalyses.stream()
                            .filter(a -> a.getAdmissionNumber()
                                    .equals(student.getAdmissionNumber()))
                            .toList();

                    String overallRisk = determineOverallRisk(studentAnalyses);
                    double avgRisk = studentAnalyses.stream()
                            .filter(a -> a.getRiskPercentage() != null)
                            .mapToDouble(AiAnalysis::getRiskPercentage)
                            .average().orElse(0.0);

                    return ClassReportDTO.StudentSummaryDTO.builder()
                            .admissionNumber(student.getAdmissionNumber())
                            .fullName(student.getFullName())
                            .overallRiskLevel(overallRisk)
                            .averageRiskScore(
                                    Math.round(avgRisk * 10.0) / 10.0)
                            .build();
                }).toList();

        // ── Risk counts ──
        long high = allAnalyses.stream()
                .filter(a -> "HIGH".equals(a.getRiskLevel())).count();
        long medium = allAnalyses.stream()
                .filter(a -> "MEDIUM".equals(a.getRiskLevel())).count();
        long low = allAnalyses.stream()
                .filter(a -> "LOW".equals(a.getRiskLevel())).count();

        return ClassReportDTO.builder()
                .className(className)
                .totalStudents(students.size())
                .analyzedStudents(
                        (int) admissionNumbers.stream()
                                .filter(adm -> allAnalyses.stream()
                                        .anyMatch(a -> a.getAdmissionNumber().equals(adm)))
                                .count())
                .subjectWeaknesses(subjectWeaknesses)
                .studentSummaries(studentSummaries)
                .highRiskCount(high)
                .mediumRiskCount(medium)
                .lowRiskCount(low)
                .build();
    }

    // ── School overview ──
    public SchoolOverviewDTO getSchoolOverview(Term term, Integer academicYear) {
        List<Student> allStudents = studentService.getAllStudents()
                .stream()
                .map(s -> studentService.findOrThrow(s.getAdmissionNumber()))
                .toList();

        // Group students by class
        Map<String, List<Student>> byClass = allStudents.stream()
                .collect(Collectors.groupingBy(Student::getClassName));

        List<SchoolOverviewDTO.ClassSummaryDTO> classSummaries =
                new ArrayList<>();

        long totalHigh = 0, totalMedium = 0, totalLow = 0;

        for (Map.Entry<String, List<Student>> entry : byClass.entrySet()) {
            String className = entry.getKey();
            List<String> admNos = entry.getValue().stream()
                    .map(Student::getAdmissionNumber).toList();

            List<AiAnalysis> analyses = analysisRepository
                    .findByStudentsAndTerm(admNos, term, academicYear);

            long high = analyses.stream()
                    .filter(a -> "HIGH".equals(a.getRiskLevel())).count();
            long medium = analyses.stream()
                    .filter(a -> "MEDIUM".equals(a.getRiskLevel())).count();
            long low = analyses.stream()
                    .filter(a -> "LOW".equals(a.getRiskLevel())).count();

            totalHigh += high;
            totalMedium += medium;
            totalLow += low;

            double avgRisk = analyses.stream()
                    .filter(a -> a.getRiskPercentage() != null)
                    .mapToDouble(AiAnalysis::getRiskPercentage)
                    .average().orElse(0.0);

            // Find most weak subject in this class
            String mostWeak = analyses.stream()
                    .collect(Collectors.groupingBy(
                            AiAnalysis::getSubjectId, Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> {
                        try {
                            return subjectService.getById(e.getKey())
                                    .getSubjectName();
                        } catch (Exception ex) {
                            return "Unknown";
                        }
                    }).orElse("N/A");

            classSummaries.add(SchoolOverviewDTO.ClassSummaryDTO.builder()
                    .className(className)
                    .totalStudents(entry.getValue().size())
                    .highRiskCount(high)
                    .averageRiskScore(Math.round(avgRisk * 10.0) / 10.0)
                    .mostWeakSubject(mostWeak)
                    .build());
        }

        return SchoolOverviewDTO.builder()
                .totalStudents(allStudents.size())
                .totalClasses(byClass.size())
                .totalHighRisk(totalHigh)
                .totalMediumRisk(totalMedium)
                .totalLowRisk(totalLow)
                .classSummaries(classSummaries)
                .build();
    }

    // ── Helper ──
    private String determineOverallRisk(List<AiAnalysis> analyses) {
        if (analyses.stream().anyMatch(a -> "HIGH".equals(a.getRiskLevel())))
            return "HIGH";
        if (analyses.stream().anyMatch(a -> "MEDIUM".equals(a.getRiskLevel())))
            return "MEDIUM";
        return "LOW";
    }
}