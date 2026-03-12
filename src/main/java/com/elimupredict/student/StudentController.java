package com.elimupredict.student;

import com.elimupredict.student.dto.StudentRequest;
import com.elimupredict.student.dto.StudentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('IT_HANDLER', 'ADMIN','DEPUTY_PRINCIPAL','SENIOR_TEACHER')")
    public ResponseEntity<StudentResponse> registerStudent(@Valid @RequestBody StudentRequest request){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentService.registerStudent(request));
    }

    @GetMapping("/{admissionNumber}")
    @PreAuthorize("hasAnyRole('IT_HANDLER', 'ADMIN','DEPUTY_PRINCIPAL','PRINCIPAL','SENIOR_TEACHER','TEACHER')")
    public ResponseEntity<StudentResponse> getStudentByAdmissionNumber(@PathVariable String admissionNumber) {
        return ResponseEntity.ok(studentService.getStudentByAdmissionNumber(admissionNumber));
    }

    @GetMapping("/class/{className}")
    @PreAuthorize("hasAnyRole('IT_HANDLER', 'ADMIN','DEPUTY_PRINCIPAL','PRINCIPAL','TEACHER','SENIOR_TEACHER')")
    public ResponseEntity<List<StudentResponse>> getStudentsByClassName(@PathVariable String className) {
        return ResponseEntity.ok(studentService.getStudentsByClassName(className));
    }

    @GetMapping("/parent/{parentId}")
    @PreAuthorize("hasAnyRole('IT_HANDLER', 'ADMIN','DEPUTY_PRINCIPAL','PRINCIPAL','TEACHER','PARENT')")
    public ResponseEntity<List<StudentResponse>> getByParentId(@PathVariable Long parentId) {
        return ResponseEntity.ok(studentService.getByParentId(parentId));
    }

    @PutMapping("/{admissionNumber}")
    @PreAuthorize("hasAnyRole('IT_HANDLER', 'ADMIN','DEPUTY_PRINCIPAL')")
    public ResponseEntity<StudentResponse> updateStudent(@PathVariable String admissionNumber, @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(admissionNumber, request));
    }
}
