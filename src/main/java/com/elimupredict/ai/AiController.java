package com.elimupredict.ai;

import com.elimupredict.ai.dto.AnalysisResponse;
import com.elimupredict.common.enums.Term;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAnalysisService aiAnalysisService;

    // ── Trigger analysis for one student ──
    @PostMapping("/analyze/student/{admissionNumber}")
    @PreAuthorize("hasAnyRole('TEACHER','SENIOR_TEACHER','PRINCIPAL','ADMIN')")
    public ResponseEntity<List<AnalysisResponse>> analyzeStudent(
            @PathVariable String admissionNumber,
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                aiAnalysisService.analyzeStudent(admissionNumber, term, academicYear));
    }

    // ── Trigger analysis for entire class ──
    @PostMapping("/analyze/class/{className}")
    @PreAuthorize("hasAnyRole('SENIOR_TEACHER','PRINCIPAL','ADMIN')")
    public ResponseEntity<Map<String, List<AnalysisResponse>>> analyzeClass(
            @PathVariable String className,
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                aiAnalysisService.analyzeClass(className, term, academicYear));
    }

    // ── Get stored results for a student ──
    @GetMapping("/results/student/{admissionNumber}")
    @PreAuthorize("hasAnyRole('TEACHER','SENIOR_TEACHER','PRINCIPAL'," +
            "'DEPUTY_PRINCIPAL','PARENT','ADMIN')")
    public ResponseEntity<List<AnalysisResponse>> getStudentResults(
            @PathVariable String admissionNumber,
            @RequestParam Term term) {
        return ResponseEntity.ok(
                aiAnalysisService.getStudentResults(admissionNumber, term));
    }

    // ── Get all results for a student across all terms ──
    @GetMapping("/results/student/{admissionNumber}/all")
    @PreAuthorize("hasAnyRole('TEACHER','SENIOR_TEACHER','PRINCIPAL','ADMIN')")
    public ResponseEntity<List<AiAnalysis>> getAllStudentResults(
            @PathVariable String admissionNumber) {
        return ResponseEntity.ok(
                aiAnalysisService.getAllStudentResults(admissionNumber));
    }
}