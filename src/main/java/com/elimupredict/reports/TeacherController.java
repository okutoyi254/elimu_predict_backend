package com.elimupredict.reports;



import com.elimupredict.common.ApiVersion;
import com.elimupredict.common.enums.Term;
import com.elimupredict.reports.dto.TeacherProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersion.V1+"/teacher")

public class TeacherController {

    private final TeacherReportService teacherReportService;

    // ── Full teacher profile
    @GetMapping("/profile")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherProfileDTO> getTeacherProfile(
            @AuthenticationPrincipal String username,
            @RequestParam Term term,
            @RequestParam Integer academicYear) {
        return ResponseEntity.ok(
                teacherReportService.getTeacherProfile(
                        username, term, academicYear));
    }

    // Single subject card
    @GetMapping("/profile/subject/{subjectCode}/class/{className}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherProfileDTO.SubjectCardDTO> getSubjectCard(
            @AuthenticationPrincipal String username,
            @PathVariable String subjectCode,
            @PathVariable String className,
            @RequestParam Term term,
            @RequestParam Integer academicYear) {

        TeacherProfileDTO profile = teacherReportService
                .getTeacherProfile(username, term, academicYear);

        return profile.getSubjectCards().stream()
                .filter(c -> c.getSubjectCode().equals(subjectCode)
                        && c.getClassName().equals(className))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException(
                        "Subject card not found for subject " + subjectCode +
                                " in class " + className));
    }
}