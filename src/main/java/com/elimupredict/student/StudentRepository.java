package com.elimupredict.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student,Long> {

    Optional<Student> findByAdmissionNumber(String admissionNumber);
    boolean existsByAdmissionNumber(String admissionNumber);
    List<Student> findByClassName(String className);
    List<Student>findByClassNameAndIsActiveTrue(String className);
    List<Student>findByParentId(Long parentId);
    Optional<Student>findByAdmissionNumberAndIsActiveTrue(String admissionNumber);

}
