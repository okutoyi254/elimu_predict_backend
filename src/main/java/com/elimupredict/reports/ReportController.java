package com.elimupredict.reports;

import com.elimupredict.common.ApiVersion;
import com.elimupredict.common.enums.Term;
import com.elimupredict.reports.dto.*;
import com.elimupredict.student.StudentService;
import com.elimupredict.user.User;
import com.elimupredict.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1)
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final StudentService studentService;

    // ── Student report ──
    @GetMapping("/dashboard/parent/child/{admissionNumber}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<StudentReportDTO> getChildReport(
            @AuthenticationPrincipal String userId,   // from JWT — not URL
            @PathVariable String admissionNumber,
            @RequestParam Term term,
            @RequestParam Integer academicYear) {

        User parent = userRepository.findByUsername(userId)
                .orElseThrow(() -> new RuntimeException(
                        "Parent not found: " + userId));

        // Verify this child actually belongs to this parent
        boolean isParentsChild = studentService
                .getByParentId(parent.getId())
                .stream()
                .anyMatch(s -> s.getAdmissionNumber().equals(admissionNumber));

        if (!isParentsChild) {
            throw new RuntimeException(
                    "Student " + admissionNumber +
                            " is not linked to your account.");
        }

        return ResponseEntity.ok(
                reportService.getStudentReport(
                        admissionNumber, term, academicYear));
    }

    // ── Class report ──
    @GetMapping("/reports/class/{className}")
    @PreAuthorize("hasAnyRole('TEACHER','SENIOR_TEACHER','PRINCIPAL'," +
            "'DEPUTY_PRINCIPAL','ADMIN')")
    public ResponseEntity<ClassReportDTO> getClassReport(
            @PathVariable String className,
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                reportService.getClassReport(className, term, academicYear));
    }

    // ── School overview ──
    @GetMapping("/reports/school")
    @PreAuthorize("hasAnyRole('PRINCIPAL','DEPUTY_PRINCIPAL','ADMIN')")
    public ResponseEntity<SchoolOverviewDTO> getSchoolOverview(
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                reportService.getSchoolOverview(term, academicYear));
    }

    // ── Teacher dashboard ──
    @GetMapping("/dashboard/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherDashboardDTO> getTeacherDashboard(
            @AuthenticationPrincipal String userId,
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                dashboardService.getTeacherDashboard(userId, term, academicYear));
    }

    // ── Senior teacher dashboard ──
    @GetMapping("/dashboard/senior")
    @PreAuthorize("hasRole('SENIOR_TEACHER')")
    public ResponseEntity<SeniorDashboardDTO> getSeniorDashboard(
            @AuthenticationPrincipal String userId,
            @RequestParam String className,
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                dashboardService.getSeniorDashboard(userId,className, term, academicYear));
    }

    // ── Principal / Deputy dashboard ──
    @GetMapping("/dashboard/principal")
    @PreAuthorize("hasAnyRole('PRINCIPAL','DEPUTY_PRINCIPAL')")
    public ResponseEntity<SchoolOverviewDTO> getPrincipalDashboard(
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                reportService.getSchoolOverview(term, academicYear));
    }

    // ── Parent dashboard ──
    @GetMapping("/dashboard/parent")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ParentDashboardDTO> getParentDashboard(
            @AuthenticationPrincipal String userId,   // from JWT token
            @RequestParam Term term,
            @RequestParam Integer academicYear) {

        User parent = userRepository.findByUsername(userId)
                .orElseThrow(() -> new RuntimeException(
                        "Parent not found: " + userId));

        return ResponseEntity.ok(
                dashboardService.getParentDashboard(
                        parent.getId(), term, academicYear));
    }
}
