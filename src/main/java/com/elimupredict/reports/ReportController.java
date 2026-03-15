package com.elimupredict.reports;

import com.elimupredict.common.enums.Term;
import com.elimupredict.reports.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final DashboardService dashboardService;

    // ── Student report ──
    @GetMapping("/reports/student/{admissionNumber}")
    @PreAuthorize("hasAnyRole('TEACHER','SENIOR_TEACHER','PRINCIPAL','ADMIN')")
    public ResponseEntity<StudentReportDTO> getStudentReport(
            @PathVariable String admissionNumber,
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                reportService.getStudentReport(admissionNumber, term, academicYear));
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
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                dashboardService.getSeniorDashboard(userId, term, academicYear));
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
    @GetMapping("/dashboard/parent/{parentId}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ParentDashboardDTO> getParentDashboard(
            @PathVariable Long parentId,
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                dashboardService.getParentDashboard(parentId, term, academicYear));
    }
}
