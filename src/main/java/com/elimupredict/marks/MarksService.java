package com.elimupredict.marks;

import com.elimupredict.common.enums.Term;
import com.elimupredict.marks.dto.BulkMarksUploadRequest;
import com.elimupredict.marks.dto.MarksUploadRequest;
import com.elimupredict.student.Student;
import com.elimupredict.student.StudentService;
import com.elimupredict.subject.Subject;
import com.elimupredict.subject.SubjectRepository;
import com.elimupredict.subject.SubjectService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarksService {

    private final StudentRecordRepository recordRepository;
    private final StudentService studentService;
    private final SubjectService subjectService;
    private final SubjectRepository subjectRepository;

    public StudentRecord uploadMarks(MarksUploadRequest request,Long uploadedBy){


//        Validate student exists
        studentService.findOrThrow(request.getAdmissionNumber());

//        Validate subject exists
        Subject subject = subjectService.getBySubjectCode(request.getSubjectCode());

//        Block future term entry
        validateNotFutureTerm(request.getTerm(),request.getAcademicYear());

//        Block duplicate entry
        if(recordRepository.existsByAdmissionNumberAndSubjectIdAndExamTypeAndTermAndAcademicYear(
                request.getAdmissionNumber(),subject.getId(),request.getExamType(),request.getTerm(),request.getAcademicYear()
        )){
            throw  new RuntimeException(
                    "Mark already exists for this student,subject, exam and term.Use update instead"
            );
        }

        StudentRecord record = StudentRecord.builder()
                .admissionNumber(request.getAdmissionNumber())
                .subjectId(subject.getId())
                .marksObtained(request.getMarksObtained())
                .totalMarks(100.0)
                .examType(request.getExamType())
                .term(request.getTerm())
                .academicYear(request.getAcademicYear())
                .uploadedBy(uploadedBy)
                .build();

        return recordRepository.save(record);
    }

    public List<StudentRecord> bulkUpload(
            BulkMarksUploadRequest request,Long uploadedBy){
        return request.getRecords().stream()
                .map(r->uploadMarks(r,uploadedBy))
                .toList();
    }

    public StudentRecord updateMarks(Long recordId,
                                     Double newMarks,Long requestedBy){

        StudentRecord record = recordRepository.findById(recordId).orElseThrow(()->new RuntimeException("Record not found: "+recordId));

        if(!record.getUploadedBy().equals(requestedBy)){
            throw new RuntimeException("You are not authorized to edit the marks");
        }

        record.setMarksObtained(newMarks);
        return recordRepository.save(record);
    }

    public List<StudentRecord> getByStudent(String admissionNumber) {
        return recordRepository.findByAdmissionNumber(admissionNumber);
    }

    public List<StudentRecord> getByStudentAndTerm(String admissionNumber, Term term) {
        return recordRepository.findByAdmissionNumberAndTerm(admissionNumber, term);
    }

    public List<StudentRecord> getClassRecords(
            String subjectCode, Term term, Integer year) {

        Subject subject = subjectRepository.findBySubjectCode(subjectCode)
                .orElseThrow(() -> new RuntimeException("Subject not found: " + subjectCode));
        return recordRepository.findClassRecords(subject.getId(), term, year);
    }

    private void validateNotFutureTerm(Term term,Integer academicYear){
        int currentYear = LocalDate.now().getYear();
        int currentMonth= LocalDate.now().getMonthValue();

        Term currentTerm = currentMonth <=4 ? Term.TERM_1
                : currentMonth <=8 ? Term.TERM_2
                        :Term.TERM_1;

        if(academicYear > currentYear){
            throw new RuntimeException("Cannot upload marks for a future academic year");
        }

        if(academicYear == currentYear && term.ordinal() >currentTerm.ordinal()){
            throw  new RuntimeException(
                    "Cannot upload marks for a future term: "+term
            );
        }
    }
}
