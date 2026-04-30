package com.elimupredict.subject;

import com.elimupredict.subject.dto.*;
import com.elimupredict.user.User;
import com.elimupredict.user.UserRepository;
import com.elimupredict.common.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherAssignmentService {

    private final TeacherAssignmentRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;


    // ── Assign teacher to subject + class ──
    @Transactional
    public AssignmentResponse assignTeacher(
            AssignmentRequest request, String assignedBy) {

        Subject subject = subjectRepository.findBySubjectCode(request.getSubjectCode())
                .orElseThrow(() -> new RuntimeException(
                        "Subject not found with code: " + request.getSubjectCode()));

        User teacher = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException(
                        "Teacher not found with username: " + request.getUsername()));



        if (teacher.getRole() != Role.TEACHER) {
            throw new RuntimeException(
                    teacher.getUsername() + " is not a TEACHER");
        }



        // Check duplicate assignment
        if (assignmentRepository.existsByTeacherIdAndSubjectIdAndClassName(
                teacher.getId(), subject.getId(),
                request.getClassName())) {
            throw new RuntimeException(
                    teacher.getFullName() + " is already assigned to " +
                            subject.getSubjectName() + " in " + request.getClassName());
        }

        TeacherAssignment assignment = TeacherAssignment.builder()
                .teacherId(teacher.getId())
                .subjectId(subject.getId())
                .className(request.getClassName())
                .isActive(true)
                .assignedBy(assignedBy)
                .build();

        TeacherAssignment saved = assignmentRepository.save(assignment);

        log.info("[ASSIGNMENT] {} assigned to {} in {} by {}",
                teacher.getFullName(), subject.getSubjectName(),
                request.getClassName(), assignedBy);

        return toResponse(saved, teacher, subject);
    }

    // ── Revoke assignment ──
    @Transactional
    public AssignmentResponse revokeAssignment(Long assignmentId) {
        TeacherAssignment assignment = assignmentRepository
                .findById(assignmentId)
                .orElseThrow(() -> new RuntimeException(
                        "Assignment not found: " + assignmentId));

        assignment.setIsActive(false);
        TeacherAssignment saved = assignmentRepository.save(assignment);

        User teacher = userRepository.findById(assignment.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        Subject subject = subjectRepository.findById(assignment.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        log.info("[ASSIGNMENT] Revoked: {} from {} in {}",
                teacher.getFullName(), subject.getSubjectName(),
                assignment.getClassName());

        return toResponse(saved, teacher, subject);
    }

    // ── Get all assignments for a teacher ──
    public List<AssignmentResponse> getTeacherAssignments(Long teacherId) {
        return assignmentRepository
                .findByTeacherIdAndIsActiveTrue(teacherId)
                .stream()
                .map(a -> {
                    User teacher = userRepository.findById(a.getTeacherId())
                            .orElseThrow();
                    Subject subject = subjectRepository
                            .findById(a.getSubjectId()).orElseThrow();
                    return toResponse(a, teacher, subject);
                })
                .toList();
    }

    // ── Get teacher's classes + subjects for dashboard dropdown ──
    public TeacherClassSubjectDTO getTeacherClassesAndSubjects(String userId) {
        User teacher = userRepository.findByUsername(userId)
                .orElseThrow(() -> new RuntimeException(
                        "Teacher not found: " + userId));

        // Get all active assignments
        List<TeacherAssignment> assignments = assignmentRepository
                .findByTeacherIdAndIsActiveTrue(teacher.getId());

        if (assignments.isEmpty()) {
            throw new RuntimeException(
                    "No classes assigned to teacher: " + userId +
                            ". Ask IT Handler to assign you to a class.");
        }

        // Group by class
        List<TeacherClassSubjectDTO.ClassSubjectDTO> classSubjects =
                assignments.stream()
                        .collect(Collectors.groupingBy(TeacherAssignment::getClassName))
                        .entrySet().stream()
                        .map(entry -> {
                            String className = entry.getKey();
                            List<TeacherClassSubjectDTO.SubjectOptionDTO> subjects =
                                    entry.getValue().stream()
                                            .map(a -> {
                                                Subject s = subjectRepository
                                                        .findById(a.getSubjectId())
                                                        .orElseThrow();
                                                return TeacherClassSubjectDTO.SubjectOptionDTO
                                                        .builder()
                                                        .subjectId(s.getId())
                                                        .subjectName(s.getSubjectName())
                                                        .subjectCode(s.getSubjectCode())
                                                        .build();
                                            })
                                            .toList();

                            return TeacherClassSubjectDTO.ClassSubjectDTO.builder()
                                    .className(className)
                                    .subjects(subjects)
                                    .build();
                        })
                        .toList();

        List<String> availableClasses = classSubjects.stream()
                .map(TeacherClassSubjectDTO.ClassSubjectDTO::getClassName)
                .sorted()
                .toList();

        return TeacherClassSubjectDTO.builder()
                .teacherName(teacher.getFullName())
                .availableClasses(availableClasses)
                .classSubjects(classSubjects)
                .build();
    }

    // ── Get all assignments (admin view) ──
    public List<AssignmentResponse> getAllAssignments() {
        return assignmentRepository.findAll()
                .stream()
                .map(a -> {
                    User teacher = userRepository
                            .findById(a.getTeacherId()).orElseThrow();
                    Subject subject = subjectRepository
                            .findById(a.getSubjectId()).orElseThrow();
                    return toResponse(a, teacher, subject);
                })
                .toList();
    }

    private AssignmentResponse toResponse(
            TeacherAssignment a, User teacher, Subject subject) {
        return AssignmentResponse.builder()
                .teacherName(teacher.getFullName())
                .subjectName(subject.getSubjectName())
                .className(a.getClassName())
                .isActive(a.getIsActive())
                .assignedAt(a.getAssignedAt())
                .build();
    }
}