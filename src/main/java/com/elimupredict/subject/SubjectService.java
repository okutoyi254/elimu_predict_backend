package com.elimupredict.subject;

import com.elimupredict.subject.dto.SubjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public Subject createSubject(SubjectRequest request){

        if(subjectRepository.existsBySubjectCode(request.getSubjectCode())){
            throw new RuntimeException("Subject code already exists: "+request.getSubjectCode());
        }
        Subject subject = Subject.builder()
                .subjectCode(request.getSubjectCode())
                .subjectName(request.getSubjectName())
                .teacherId(request.getTeacherId())
                .className(request.getClassName())
                .isActive(true)
                .build();
        return subjectRepository.save(subject);
    }

    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    public List<Subject> getByClass(String className) {
        return subjectRepository.findByClassName(className);
    }

    public Subject getById(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found: " + id));
    }
    public List<Subject> getByTeacher(Long teacherId) {
        return subjectRepository.findByTeacherId(teacherId);
    }
}
