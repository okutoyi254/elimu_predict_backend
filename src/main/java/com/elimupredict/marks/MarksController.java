package com.elimupredict.marks;

import com.elimupredict.auth.user.UserRepository;
import com.elimupredict.common.enums.Term;
import com.elimupredict.marks.dto.BulkMarksUploadRequest;
import com.elimupredict.marks.dto.MarksUploadRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/marks")
public class MarksController {

    private final MarksService marksService;
    private final UserRepository userRepository;

    private Long resolveName(String userName){

        return userRepository.findByUserName(userName)
                .orElseThrow(()->new RuntimeException("Invalid username!"))
                .getId();
    }

    @PostMapping
    @PreAuthorize(("hasRole('TEACHER')"))
    public ResponseEntity<StudentRecord>upload(
            @Valid @RequestBody MarksUploadRequest request,
            @AuthenticationPrincipal String userName){
        return  ResponseEntity.status(HttpStatus.CREATED)
                .body(marksService.uploadMarks(request,resolveName(userName)));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<StudentRecord>> bulkUpload(
            @Valid @RequestBody BulkMarksUploadRequest request
            ,@AuthenticationPrincipal String userName
            ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(marksService.bulkUpload(request,resolveName(userName)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<StudentRecord> update(
            @PathVariable Long id,
            @RequestParam Double marks,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(
                marksService.updateMarks(id, marks, resolveName(userId)));
    }


    @GetMapping("/student/{admissionNumber}")
    @PreAuthorize("hasAnyRole('TEACHER','SENIOR_TEACHER','PRINCIPAL','DEPUTY_PRINCIPAL')")
    public ResponseEntity<List<StudentRecord>> getByStudent(
            @PathVariable String admissionNumber) {
        return ResponseEntity.ok(marksService.getByStudent(admissionNumber));
    }

    @GetMapping("/student/{admissionNumber}/term/{term}")
    @PreAuthorize("hasAnyRole('TEACHER','SENIOR_TEACHER','PRINCIPAL')")
    public ResponseEntity<List<StudentRecord>> getByStudentAndTerm(
            @PathVariable String admissionNumber,
            @PathVariable Term term) {
        return ResponseEntity.ok(
                marksService.getByStudentAndTerm(admissionNumber, term));
    }

    @GetMapping("/class/subject/{subjectId}/term/{term}/year/{year}")
    @PreAuthorize("hasAnyRole('TEACHER','SENIOR_TEACHER','PRINCIPAL')")
    public ResponseEntity<List<StudentRecord>> getClassRecords(
            @PathVariable Long subjectId,
            @PathVariable Term term,
            @PathVariable Integer year) {
        return ResponseEntity.ok(
                marksService.getClassRecords(subjectId, term, year));
    }
}



