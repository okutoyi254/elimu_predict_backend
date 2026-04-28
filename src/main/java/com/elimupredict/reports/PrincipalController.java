package com.elimupredict.reports;

import com.elimupredict.common.enums.Term;
import com.elimupredict.reports.dto.PrincipalDashboardDTO;
import com.elimupredict.reports.dto.SchoolAnalysisDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/principal")
@RequiredArgsConstructor
public class PrincipalController {

    private final PrincipalReportService principalReportService;

    // ── Main dashboard
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('PRINCIPAL','DEPUTY_PRINCIPAL','ADMIN')")
    public ResponseEntity<PrincipalDashboardDTO> getDashboard(
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                principalReportService.getPrincipalDashboard(
                        term, academicYear));
    }

    // General analysis tab
    @GetMapping("/analysis")
    @PreAuthorize("hasAnyRole('PRINCIPAL','DEPUTY_PRINCIPAL','ADMIN')")
    public ResponseEntity<SchoolAnalysisDTO> getSchoolAnalysis(
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                principalReportService.getSchoolAnalysis(
                        term, academicYear));
    }
}