package com.elimupredict.reports;

import com.elimupredict.ai.AiAnalysis;
import com.elimupredict.auth.user.User;
import com.elimupredict.auth.user.UserRepository;
import com.elimupredict.common.enums.Term;
import com.elimupredict.reports.dto.*;
import com.elimupredict.ai.AIAnalysisRepository;
import com.elimupredict.student.Student;
import com.elimupredict.student.StudentService;
import com.elimupredict.subject.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AIAnalysisRepository analysisRepository;
    private final StudentService studentService;
    private final SubjectService subjectService;
    private final UserRepository userRepository;
    private final ReportService reportService;

    // ── Teacher dashboard ──
    public TeacherDashboardDTO getTeacherDashboard(
            String teacherId, Term term, Integer academicYear) {

        User teacher = userRepository.findByUserName(teacherId)
                .orElseThrow(() -> new RuntimeException(
                        "Teacher not found: " + teacherId));

        // Get subjects assigned to this teacher
        List<Long> subjectIds = subjectService
                .getByTeacher(teacher.getId())
                .stream().map(s -> s.getId()).toList();

        if (subjectIds.isEmpty()) {
            throw new RuntimeException(
                    "No subjects assigned to teacher: " + teacherId);
        }

        // Get class from first subject
        String className = subjectService
                .getByTeacher(teacher.getId())
                .get(0).getClassName();

        // Get all students in class
        List<Student> students = studentService.getStudentsByClassName(className)
                .stream()
                .map(s -> studentService.findOrThrow(s.getAdmissionNumber()))
                .toList();

        List<String> admNos = students.stream()
                .map(Student::getAdmissionNumber).toList();

        List<AiAnalysis> analyses = analysisRepository
                .findByStudentsAndTerm(admNos, term, academicYear);

        // Build at-risk student list
        List<TeacherDashboardDTO.AtRiskStudentDTO> atRisk = analyses.stream()
                .filter(a -> "HIGH".equals(a.getRiskLevel())
                        || "MEDIUM".equals(a.getRiskLevel()))
                .map(a -> {
                    Student s = studentService.findOrThrow(a.getAdmissionNumber());
                    String subjectName = "Unknown";
                    try {
                        subjectName = subjectService.getById(a.getSubjectId())
                                .getSubjectName();
                    } catch (Exception ignored) {}

                    return TeacherDashboardDTO.AtRiskStudentDTO.builder()
                            .admissionNumber(a.getAdmissionNumber())
                            .fullName(s.getFullName())
                            .riskLevel(a.getRiskLevel())
                            .riskPercentage(a.getRiskPercentage())
                            .weakestSubject(subjectName)
                            .suggestion(a.getSuggestion())
                            .build();
                })
                .sorted(Comparator.comparingDouble(
                                TeacherDashboardDTO.AtRiskStudentDTO::getRiskPercentage)
                        .reversed())
                .toList();

        long highCount = atRisk.stream()
                .filter(a -> "HIGH".equals(a.getRiskLevel())).count();
        long medCount = atRisk.stream()
                .filter(a -> "MEDIUM".equals(a.getRiskLevel())).count();

        // Class weaknesses
        ClassReportDTO classReport = reportService
                .getClassReport(className, term, academicYear);

        return TeacherDashboardDTO.builder()
                .teacherId(teacherId)
                .teacherName(teacher.getFullName())
                .atRiskStudents(atRisk)
                .highRiskCount(highCount)
                .mediumRiskCount(medCount)
                .classWeaknesses(classReport.getSubjectWeaknesses())
                .build();
    }

    // ── Senior Teacher dashboard ──
    public SeniorDashboardDTO getSeniorDashboard(
            String userId, Term term, Integer academicYear) {

        ClassReportDTO classReport = reportService
                .getClassReport("Form 3A", term, academicYear);

        // Build resource allocation recommendations
        List<SeniorDashboardDTO.ResourceAllocationDTO> recommendations =
                classReport.getSubjectWeaknesses().stream()
                        .map(w -> {
                            String priority = w.getWeaknessPercentage() >= 60 ? "HIGH"
                                    : w.getWeaknessPercentage() >= 30 ? "MEDIUM"
                                    : "LOW";

                            String action = w.getWeaknessPercentage() >= 60
                                    ? "Allocate additional teacher and study materials"
                                    : w.getWeaknessPercentage() >= 30
                                    ? "Schedule extra revision sessions"
                                    : "Monitor and maintain current resources";

                            return SeniorDashboardDTO.ResourceAllocationDTO.builder()
                                    .subjectName(w.getSubjectName())
                                    .affectedStudents(w.getAffectedStudents())
                                    .recommendation(action)
                                    .priority(priority)
                                    .build();
                        })
                        .toList();

        return SeniorDashboardDTO.builder()
                .seniorTeacherId(userId)
                .resourceRecommendations(recommendations)
                .overallWeaknesses(classReport.getSubjectWeaknesses())
                .totalAtRiskStudents(
                        classReport.getHighRiskCount() +
                                classReport.getMediumRiskCount())
                .build();
    }

    // ── Parent dashboard ──
    public ParentDashboardDTO getParentDashboard(
            Long parentId, Term term, Integer academicYear) {

        List<Student> children = studentService.getByParentId(parentId)
                .stream()
                .map(s -> studentService.findOrThrow(s.getAdmissionNumber()))
                .toList();

        if (children.isEmpty()) {
            throw new RuntimeException(
                    "No student linked to parent ID: " + parentId);
        }

        // Take first child (one parent = one child in this system)
        Student child = children.get(0);

        StudentReportDTO report = reportService.getStudentReport(
                child.getAdmissionNumber(), term, academicYear);

        String message = buildParentMessage(report.getOverallRiskLevel(),
                child.getFullName());

        return ParentDashboardDTO.builder()
                .parentId(parentId.toString())
                .childName(child.getFullName())
                .admissionNumber(child.getAdmissionNumber())
                .className(child.getClassName())
                .overallRiskLevel(report.getOverallRiskLevel())
                .overallMessage(message)
                .subjectBreakdown(report.getSubjectRisks())
                .build();
    }

    private String buildParentMessage(String riskLevel, String name) {
        return switch (riskLevel) {
            case "HIGH" -> name + " needs urgent academic support. " +
                    "Please review the subject recommendations below " +
                    "and consider extra tuition.";
            case "MEDIUM" -> name + " is showing some areas of concern. " +
                    "Review the suggestions below to help improve performance.";
            default -> name + " is performing well. " +
                    "Keep encouraging consistent study habits.";
        };
    }
}