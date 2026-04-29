package com.elimupredict.reports;

import com.elimupredict.common.ApiVersion;
import com.elimupredict.common.enums.Term;
import com.elimupredict.reports.dto.ParentStudentProfileDTO;
import com.elimupredict.reports.dto.SuggestionTabDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersion.V1+"/parent")
public class ParentController {

    private final ParentReportService parentReportService;

    @GetMapping("/children")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<ParentStudentProfileDTO>> getChildrenProfiles(
            @AuthenticationPrincipal String userId,
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                parentReportService.getChildrenProfiles(
                        userId, term, academicYear));
    }

    @GetMapping("/children/{admissionNumber}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ParentStudentProfileDTO> getChildProfile(
            @AuthenticationPrincipal String userId,
            @PathVariable String admissionNumber,
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                parentReportService.getChildFullProfile(
                        userId, admissionNumber, term, academicYear));
    }

    @GetMapping("/children/{admissionNumber}/suggestions")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<SuggestionTabDTO> getSuggestions(
            @AuthenticationPrincipal String userId,
            @PathVariable String admissionNumber,
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                parentReportService.getSuggestions(
                        userId, admissionNumber, term, academicYear));
    }
}
