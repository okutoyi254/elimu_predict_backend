package com.elimupredict.subject;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherAssignmentRepository
        extends JpaRepository<TeacherAssignment, Long> {

    List<TeacherAssignment> findByTeacherIdAndIsActiveTrue(Long teacherId);

    List<TeacherAssignment> findBySubjectIdAndClassNameAndIsActiveTrue(
            Long subjectId, String className);

    @Query("SELECT DISTINCT ta.className FROM TeacherAssignment ta " +
            "WHERE ta.teacherId = :teacherId AND ta.isActive = true")
    List<String> findClassesByTeacherId(Long teacherId);

    @Query("SELECT ta.subjectId FROM TeacherAssignment ta " +
            "WHERE ta.teacherId = :teacherId " +
            "AND ta.className = :className AND ta.isActive = true")
    List<Long> findSubjectIdsByTeacherIdAndClassName(
            Long teacherId, String className);

    boolean existsByTeacherIdAndSubjectIdAndClassName(
            Long teacherId, Long subjectId, String className);

    Optional<TeacherAssignment> findByTeacherIdAndSubjectIdAndClassName(
            Long teacherId, Long subjectId, String className);
}