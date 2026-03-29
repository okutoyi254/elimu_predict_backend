package com.elimupredict.reports;

import com.elimupredict.ai.AiAnalysis;
import com.elimupredict.common.enums.Role;
import com.elimupredict.user.User;
import com.elimupredict.user.UserRepository;
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

        User teacher = userRepository.findByUsername(teacherId)
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
                    } catch (Exception ignored) {
                    }

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
            String userId,String className, Term term, Integer academicYear) {

        ClassReportDTO classReport = reportService
                .getClassReport(className, term, academicYear);

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
                .className(className)
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

//        Validate parent exists
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException(
                        "Parent not found: " + parentId));

        if (parent.getRole() != Role.PARENT) {
            throw new RuntimeException(
                    "User " + parent.getUsername() + " is not a PARENT");
        }

        List<Student> children = studentService.getByParentId(parentId)
                .stream()
                .map(s -> studentService.findOrThrow(s.getAdmissionNumber()))
                .toList();

        if (children.isEmpty()) {
            throw new RuntimeException(
                    "No students linked to parent ID: " + parentId +
                            ". Ask IT Handler to link your children to your account.");
        }

//        Build summary for each student
        List<ParentDashboardDTO.ChildSummaryDTO> childSummaries = children.stream().map(
                child -> {

                    try {
                        // Get full AI analysis for this child
                        StudentReportDTO report = reportService.getStudentReport(
                                child.getAdmissionNumber(), term, academicYear);

                        // Get trend from latest analysis
                        Integer trend = analysisRepository
                                .findByAdmissionNumberAndTerm(
                                        child.getAdmissionNumber(), term)
                                .stream()
                                .filter(a -> a.getTrend() != null)
                                .mapToInt(AiAnalysis::getTrend)
                                .findFirst()
                                .orElse(0);

                        return ParentDashboardDTO.ChildSummaryDTO.builder()
                                .admissionNumber(child.getAdmissionNumber())
                                .fullName(child.getFullName())
                                .className(child.getClassName())
                                .enrollmentYear(child.getEnrollmentYear())
                                .overallRiskLevel(report.getOverallRiskLevel())
                                .overallMessage(buildParentMessage(
                                        report.getOverallRiskLevel(), child.getFullName()))
                                .averageRiskScore(report.getAverageRiskScore())
                                .trend(trend)
                                .subjectBreakdown(report.getSubjectRisks())  // full detail
                                .build();

                    } catch (Exception e) {
                        // Child not yet analyzed — return profile only
                        return ParentDashboardDTO.ChildSummaryDTO.builder()
                                .admissionNumber(child.getAdmissionNumber())
                                .fullName(child.getFullName())
                                .className(child.getClassName())
                                .enrollmentYear(child.getEnrollmentYear())
                                .overallRiskLevel("NOT_ANALYZED")
                                .overallMessage(child.getFullName() +
                                        "'s results have not been analyzed yet. " +
                                        "Please wait for the teacher to run analysis.")
                                .build();
                    }
                }).toList();

        return ParentDashboardDTO.builder()
                .parentId(parentId.toString())
                .totalChildren(childSummaries.size())
                .children(childSummaries)
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