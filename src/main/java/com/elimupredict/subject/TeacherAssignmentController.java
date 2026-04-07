package com.elimupredict.subject;

import com.elimupredict.subject.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
public class TeacherAssignmentController {

    private final TeacherAssignmentService assignmentService;

    // ── IT_HANDLER assigns teacher to subject + class ──
    @PostMapping
    @PreAuthorize("hasAnyRole('IT_HANDLER','ADMIN')")
    public ResponseEntity<AssignmentResponse> assign(
            @Valid @RequestBody AssignmentRequest request,
            @AuthenticationPrincipal String assignedBy) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.assignTeacher(request, assignedBy));
    }

    // ── Revoke an assignment ──
    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('IT_HANDLER','ADMIN')")
    public ResponseEntity<AssignmentResponse> revoke(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.revokeAssignment(id));
    }

    // ── Teacher gets their classes + subjects for dashboard dropdown ──
    @GetMapping("/my-classes")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherClassSubjectDTO> getMyClassesAndSubjects(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(
                assignmentService.getTeacherClassesAndSubjects(userId));
    }

    // ── Get assignments for a specific teacher (admin/IT view) ──
    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('IT_HANDLER','ADMIN','SENIOR_TEACHER')")
    public ResponseEntity<List<AssignmentResponse>> getTeacherAssignments(
            @PathVariable Long teacherId) {
        return ResponseEntity.ok(
                assignmentService.getTeacherAssignments(teacherId));
    }

    // ── Get all assignments ──
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','IT_HANDLER')")
    public ResponseEntity<List<AssignmentResponse>> getAll() {
        return ResponseEntity.ok(assignmentService.getAllAssignments());
    }
}
