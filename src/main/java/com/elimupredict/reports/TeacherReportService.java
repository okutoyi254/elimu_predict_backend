package com.elimupredict.reports;

import com.elimupredict.ai.AIAnalysisRepository;
import com.elimupredict.ai.AiAnalysis;
import com.elimupredict.common.enums.ExamType;
import com.elimupredict.common.enums.Term;
import com.elimupredict.marks.StudentRecord;
import com.elimupredict.marks.StudentRecordRepository;
import com.elimupredict.reports.dto.TeacherProfileDTO;
import com.elimupredict.student.Student;
import com.elimupredict.student.StudentService;
import com.elimupredict.subject.Subject;
import com.elimupredict.subject.SubjectService;
import com.elimupredict.subject.TeacherAssignment;
import com.elimupredict.subject.TeacherAssignmentRepository;
import com.elimupredict.user.User;
import com.elimupredict.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherReportService {

    private final UserRepository userRepository;
    private final TeacherAssignmentRepository assignmentRepository;
    private final SubjectService subjectService;
    private final StudentService studentService;
    private final StudentRecordRepository recordRepository;
    private final AIAnalysisRepository analysisRepository;

    public TeacherProfileDTO getTeacherProfile(String username, Term term, Integer academicYear){

        User teacher = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "Teacher not found: " + username));

        // Get all active assignments for this teacher
        List<TeacherAssignment> assignments = assignmentRepository
                .findByTeacherIdAndIsActiveTrue(teacher.getId());

        if (assignments.isEmpty()) {
            throw new RuntimeException(
                    "No subjects assigned to teacher: " + username +
                            ". Ask IT Handler to assign subjects.");
        }

        // Build one card per assignment
        List<TeacherProfileDTO.SubjectCardDTO> subjectCards =
                assignments.stream()
                        .map(a -> buildSubjectCard(
                                a, term, academicYear))
                        .toList();

        // Summary stats
        Set<String> uniqueClasses = assignments.stream()
                .map(TeacherAssignment::getClassName)
                .collect(Collectors.toSet());

        int totalStudents = uniqueClasses.stream()
                .mapToInt(c -> studentService.getStudentsByClassName(c).size())
                .sum();

        return TeacherProfileDTO.builder()
                .teacherName(teacher.getFullName())
                .totalAssignments(assignments.size())
                .totalClassesTaught(uniqueClasses.size())
                .totalStudentsTaught(totalStudents)
                .subjectCards(subjectCards)
                .build();
    }

    private TeacherProfileDTO.SubjectCardDTO buildSubjectCard(
            TeacherAssignment assignment,
            Term term, Integer academicYear) {

        Long subjectId = assignment.getSubjectId();
        String className = assignment.getClassName();
        Subject subject = subjectService.getById(subjectId);

        // All students in this class
        List<Student> students = studentService
                .getStudentsByClassName(className)
                .stream()
                .map(s -> studentService.findOrThrow(s.getAdmissionNumber()))
                .toList();

        List<String> admNos = students.stream()
                .map(Student::getAdmissionNumber)
                .toList();

        // Analyses for this subject in this class
        List<AiAnalysis> analyses = analysisRepository
                .findByStudentsAndTerm(admNos, term, academicYear)
                .stream()
                .filter(a -> a.getSubjectId().equals(subjectId))
                .toList();

        // Previous term
        Term prevTerm = term == Term.TERM_1 ? Term.TERM_3
                : term == Term.TERM_2 ? Term.TERM_1 : Term.TERM_2;
        int prevYear = term == Term.TERM_1 ? academicYear - 1 : academicYear;

        // Class average mark for this subject
        double classAvgMark = admNos.stream()
                .mapToDouble(admNo -> subjectMean(
                        admNo, subjectId, term, academicYear))
                .average().orElse(0.0);

        double prevAvgMark = admNos.stream()
                .mapToDouble(admNo -> subjectMean(
                        admNo, subjectId, prevTerm, prevYear))
                .average().orElse(0.0);

        double avgChange = prevAvgMark > 0 ?
                classAvgMark - prevAvgMark : 0;

        String trend = Math.abs(avgChange) < 2 ? "STABLE"
                : avgChange > 0 ? "IMPROVING" : "DECLINING";

        String trendMsg = buildTrendMessage(
                subject.getSubjectName(), className,
                trend, avgChange, prevAvgMark, classAvgMark);

        // Risk summary
        double classMeanRisk = analyses.stream()
                .filter(a -> a.getRiskPercentage() != null)
                .mapToDouble(AiAnalysis::getRiskPercentage)
                .average().orElse(0.0);

        long highCount = analyses.stream()
                .filter(a -> "HIGH".equals(a.getRiskLevel())).count();
        long medCount = analyses.stream()
                .filter(a -> "MEDIUM".equals(a.getRiskLevel())).count();
        long lowCount = analyses.stream()
                .filter(a -> "LOW".equals(a.getRiskLevel())).count();

        String status = classMeanRisk >= 70 ? "CRITICAL"
                : classMeanRisk >= 50 ? "CONCERNING"
                : classMeanRisk >= 30 ? "MODERATE" : "HEALTHY";

        // ── Assessment graph data ──
        List<TeacherProfileDTO.SubjectCardDTO.AssessmentAvgDTO> graph =
                buildAssessmentGraph(admNos, analyses, subjectId,
                        term, academicYear);

        // ── At-risk students ──
        List<TeacherProfileDTO.SubjectCardDTO.AtRiskStudentDTO> atRisk =
                buildAtRiskList(analyses, students,
                        subjectId, term, academicYear);

        // ── Top performers ──
        List<TeacherProfileDTO.SubjectCardDTO.TopStudentDTO> topStudents =
                buildTopStudents(analyses, students,
                        subjectId, term, academicYear);

        return TeacherProfileDTO.SubjectCardDTO.builder()
                .subjectName(subject.getSubjectName())
                .subjectCode(subject.getSubjectCode())
                .className(className)
                .totalStudents(students.size())
                .analyzedStudents(analyses.stream()
                        .map(AiAnalysis::getAdmissionNumber)
                        .distinct()
                        .mapToInt(x -> 1).sum())
                .classAverageMark(Math.round(classAvgMark * 10.0) / 10.0)
                .classMeanRisk(Math.round(classMeanRisk * 10.0) / 10.0)
                .subjectStatus(status)
                .highRiskCount((int) highCount)
                .lowRiskCount((int) lowCount)
                .previousTermAvg(prevAvgMark > 0 ?
                        Math.round(prevAvgMark * 10.0) / 10.0 : null)
                .avgChange(prevAvgMark > 0 ?
                        Math.round(avgChange * 10.0) / 10.0 : null)
                .trend(trend)
                .trendMessage(trendMsg)
                .assessmentGraph(graph)
                .atRiskStudents(atRisk)
                .topStudents(topStudents)
                .build();
    }

    private List<TeacherProfileDTO.SubjectCardDTO.AssessmentAvgDTO>
    buildAssessmentGraph(
            List<String> admNos,
            List<AiAnalysis> analyses,
            Long subjectId,
            Term term, Integer academicYear) {

        // Split students by risk level for comparison lines
        Set<String> highRiskAdmNos = analyses.stream()
                .filter(a -> "HIGH".equals(a.getRiskLevel()))
                .map(AiAnalysis::getAdmissionNumber)
                .collect(Collectors.toSet());

        Set<String> lowRiskAdmNos = analyses.stream()
                .filter(a -> "LOW".equals(a.getRiskLevel()))
                .map(AiAnalysis::getAdmissionNumber)
                .collect(Collectors.toSet());

        Map<ExamType, String> labels = Map.of(
                ExamType.CAT_1,  "CAT 1",
                ExamType.CAT_2,  "CAT 2",
                ExamType.CAT_3,  "CAT 3",
                ExamType.EXAM_1, "EXAM 1",
                ExamType.EXAM_2, "EXAM 2"
        );

        List<ExamType> order = List.of(
                ExamType.CAT_1, ExamType.CAT_2, ExamType.CAT_3,
                ExamType.EXAM_1, ExamType.EXAM_2
        );

        return order.stream().map(examType -> {
            // Class average for this assessment
            double classAvg = admNos.stream()
                    .mapToDouble(admNo -> markForType(
                            admNo, subjectId, examType, term, academicYear))
                    .filter(m -> m > 0)
                    .average().orElse(0.0);

            // High risk students average
            double highAvg = highRiskAdmNos.stream()
                    .mapToDouble(admNo -> markForType(
                            admNo, subjectId, examType, term, academicYear))
                    .filter(m -> m > 0)
                    .average().orElse(0.0);

            // Low risk students average
            double lowAvg = lowRiskAdmNos.stream()
                    .mapToDouble(admNo -> markForType(
                            admNo, subjectId, examType, term, academicYear))
                    .filter(m -> m > 0)
                    .average().orElse(0.0);

            return TeacherProfileDTO.SubjectCardDTO.AssessmentAvgDTO.builder()
                    .label(labels.get(examType))
                    .classAverage(Math.round(classAvg * 10.0) / 10.0)
                    .highRiskAvg(Math.round(highAvg * 10.0) / 10.0)
                    .lowRiskAvg(Math.round(lowAvg * 10.0) / 10.0)
                    .build();
        }).toList();
    }

    private List<TeacherProfileDTO.SubjectCardDTO.AtRiskStudentDTO>
    buildAtRiskList(
            List<AiAnalysis> analyses,
            List<Student> students,
            Long subjectId,
            Term term, Integer academicYear) {

        return analyses.stream()
                .filter(a -> "HIGH".equals(a.getRiskLevel())
                        || "MEDIUM".equals(a.getRiskLevel()))
                .map(a -> {
                    Student s = students.stream()
                            .filter(st -> st.getAdmissionNumber()
                                    .equals(a.getAdmissionNumber()))
                            .findFirst().orElse(null);
                    if (s == null) return null;

                    double mean = subjectMean(
                            a.getAdmissionNumber(), subjectId,
                            term, academicYear);

                    String trendDir = a.getTrend() == null ? "STABLE"
                            : a.getTrend() > 3 ? "IMPROVING"
                            : a.getTrend() < -3 ? "DECLINING"
                            : "STABLE";

                    return TeacherProfileDTO.SubjectCardDTO
                            .AtRiskStudentDTO.builder()
                            .admissionNumber(a.getAdmissionNumber())
                            .fullName(s.getFullName())
                            .subjectMean(Math.round(mean * 10.0) / 10.0)
                            .riskPercentage(a.getRiskPercentage())
                            .riskLevel(a.getRiskLevel())
                            .trendDirection(trendDir)
                            .trend(a.getTrend())
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(
                        TeacherProfileDTO.SubjectCardDTO.AtRiskStudentDTO
                                ::getRiskPercentage).reversed())
                .toList();
    }

//    Top performers
    private List<TeacherProfileDTO.SubjectCardDTO.TopStudentDTO>
    buildTopStudents(
            List<AiAnalysis> analyses,
            List<Student> students,
            Long subjectId,
            Term term, Integer academicYear) {

        return analyses.stream()
                .filter(a -> "LOW".equals(a.getRiskLevel()))
                .map(a -> {
                    Student s = students.stream()
                            .filter(st -> st.getAdmissionNumber()
                                    .equals(a.getAdmissionNumber()))
                            .findFirst().orElse(null);
                    if (s == null) return null;

                    double mean = subjectMean(
                            a.getAdmissionNumber(), subjectId,
                            term, academicYear);

                    String trendDir = a.getTrend() == null ? "STABLE"
                            : a.getTrend() > 3 ? "IMPROVING"
                            : a.getTrend() < -3 ? "DECLINING"
                            : "STABLE";

                    return TeacherProfileDTO.SubjectCardDTO
                            .TopStudentDTO.builder()
                            .admissionNumber(a.getAdmissionNumber())
                            .fullName(s.getFullName())
                            .subjectMean(Math.round(mean * 10.0) / 10.0)
                            .riskPercentage(a.getRiskPercentage())
                            .trendDirection(trendDir)
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(
                        TeacherProfileDTO.SubjectCardDTO.TopStudentDTO
                                ::getSubjectMean).reversed())
                .limit(10)
                .toList();
    }

    private double subjectMean(String admNo, Long subjectId,
                               Term term, Integer academicYear) {
        return recordRepository
                .findByAdmissionNumberAndSubjectId(admNo, subjectId)
                .stream()
                .filter(r -> r.getTerm() == term
                        && r.getAcademicYear().equals(academicYear))
                .mapToDouble(StudentRecord::getMarksObtained)
                .average().orElse(0.0);
    }

    private double markForType(String admNo, Long subjectId,
                               ExamType type, Term term, Integer academicYear) {
        return recordRepository
                .findByAdmissionNumberAndSubjectId(admNo, subjectId)
                .stream()
                .filter(r -> r.getTerm() == term
                        && r.getAcademicYear().equals(academicYear)
                        && r.getExamType() == type)
                .mapToDouble(StudentRecord::getMarksObtained)
                .findFirst().orElse(0.0);
    }

    private String buildTrendMessage(
            String subject, String className,
            String trend, double change,
            double prev, double current) {
        return switch (trend) {
            case "IMPROVING" -> String.format(
                    "%s in %s improved — class average rose from %.1f to %.1f (+%.1f).",
                    subject, className, prev, current, change);
            case "DECLINING" -> String.format(
                    "%s in %s is declining — class average dropped from %.1f to %.1f (%.1f). " +
                            "Consider additional support.",
                    subject, className, prev, current, change);
            default -> String.format(
                    "%s in %s is stable at %.1f average.",
                    subject, className, current);
        };
    }
}
