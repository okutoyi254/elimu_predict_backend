package com.elimupredict.subject;

import com.elimupredict.subject.dto.SubjectRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','IT_HANDLER')")
    public ResponseEntity<Subject> create(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subjectService.createSubject(request));
    }

    @GetMapping
    public ResponseEntity<List<Subject>> getAll() {
        return ResponseEntity.ok(subjectService.getAllSubjects());
    }

    @GetMapping("/class/{className}")
    public ResponseEntity<List<Subject>> getByClass(@PathVariable String className) {
        return ResponseEntity.ok(subjectService.getByClass(className));
    }

}
