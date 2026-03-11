package com.elimupredict.student;

import com.elimupredict.student.dto.StudentRequest;
import com.elimupredict.student.dto.StudentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentResponse registerStudent(StudentRequest request) {
        if(studentRepository.existsByAdmissionNumber(request.getAdmissionNumber())) {
            throw new RuntimeException("Admission number already exists");
        }
        Student student = Student.builder()
                .admissionNumber(request.getAdmissionNumber())
                .fullName(request.getFullName())
                .className(request.getClassName())
                .parentId(request.getParentId())
                .enrollmentYear(request.getEnrollmentYear())
                .isActive(true)
                .build();

        return toReponse(studentRepository.save(student));
    }

    public StudentResponse getStudentByAdmissionNumber(String admissionNumber) {
        return toReponse(findOrThrow(admissionNumber));
    }

    public List<StudentResponse>getByParentId(Long parentId) {
        return studentRepository.findByParentId(parentId).stream()
                .map(this::toReponse)
                .toList();
    }

    public List<StudentResponse> getStudentsByClassName(String className) {
        return studentRepository.findByClassNameAndIsActiveTrue(className).stream()
                .map(this::toReponse)
                .toList();
    }
    public StudentResponse updateStudent(String admissionNumber, StudentRequest request) {
        Student student = findOrThrow(admissionNumber);

        student.setFullName(request.getFullName());
        student.setClassName(request.getClassName());
        student.setParentId(request.getParentId());
        student.setEnrollmentYear(request.getEnrollmentYear());

        return toReponse(studentRepository.save(student));
    }
    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(this::toReponse)
                .toList();
    }
    private Student findOrThrow(String admissionNumber) {
        return studentRepository.findByAdmissionNumber(admissionNumber)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    private StudentResponse toReponse(Student student) {
        return StudentResponse.builder()
                .admissionNumber(student.getAdmissionNumber())
                .fullName(student.getFullName())
                .className(student.getClassName())
                .parentId(student.getParentId())
                .enrollmentYear(student.getEnrollmentYear())
                .isActive(student.getIsActive())
                .createdAt(student.getCreatedAt())
                .build();
    }
}
