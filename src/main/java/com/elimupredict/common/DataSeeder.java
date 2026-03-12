package com.elimupredict.common;

import com.elimupredict.auth.user.UserRepository;
import com.elimupredict.common.enums.ExamType;
import com.elimupredict.common.enums.Role;
import com.elimupredict.common.enums.Term;
import com.elimupredict.marks.StudentRecord;
import com.elimupredict.marks.StudentRecordRepository;
import com.elimupredict.student.Student;
import com.elimupredict.student.StudentRepository;
import com.elimupredict.subject.Subject;
import com.elimupredict.subject.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRecordRepository recordRepository;

    @Override
    public void run(String... args) throws Exception {
       seedUser("ADMIN001","System Admin","admin@123", Role.ADMIN);
       seedUser("ITH001","IT Handler","ithandler@123", Role.IT_HANDLER);

       seedCsvData();
    }
    private  void seedUser(String username, String fullName, String password, Role role) {
        if (userRepository.existsByUserName(username)) {
            log.info("User {} already exists. Skipping seeding.", username);
            return;
        }

        userRepository.save(
                com.elimupredict.auth.user.User.builder()
                        .userName(username)
                        .fullName(fullName)
                        .password(passwordEncoder.encode(password))
                        .role(role)
                        .isActive(true)
                        .createdBy("SYSTEM")
                        .build()
        );
        log.info("User {} created successfully.", username);
    }

    private void seedCsvData(){

        if(recordRepository.count() > 0) return;

        Subject subject = subjectRepository.findBySubjectCode("GEN101")
                .orElseGet(()-> subjectRepository.save(Subject.builder()
                        .subjectCode("GEO101")
                        .subjectName("Geography")
                        .className("FORM 1N")
                        .isActive(true)
                        .build()
                ));

        try(BufferedReader br = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                        getClass().getResourceAsStream("/data/clean_data.csv")
                )
        ))){

            String line;
            boolean firstLine = true;
            while((line = br.readLine()) != null){
                if(firstLine) {firstLine = false; continue;
            }

                String[] cols= line.split(",");
                String admissionNo = cols[0].trim();

                // Register student if not exists
                if (!studentRepository.existsByAdmissionNumber(admissionNo)) {
                    studentRepository.save(Student.builder()
                            .admissionNumber(admissionNo)
                            .fullName("Student " + admissionNo)
                            .className("Form 1N")
                            .enrollmentYear(2024)
                            .isActive(true)
                            .build());
                }

                double[] marks = {
                        Double.parseDouble(cols[1].trim()), // cat_1
                        Double.parseDouble(cols[2].trim()), // cat_2
                        Double.parseDouble(cols[3].trim()), // cat_3
                        Double.parseDouble(cols[4].trim()), // exam_1
                        Double.parseDouble(cols[5].trim())  // exam_2
                };

                ExamType[] types ={
                        ExamType.CAT_1, ExamType.CAT_2,ExamType.CAT_3
                        ,ExamType.EXAM_1,ExamType.EXAM_2
                };

                for( int i=0; i< marks.length;i++){

                    recordRepository.save(StudentRecord.builder()
                            .admissionNumber(admissionNo)
                            .subjectId(subject.getId())
                            .marksObtained(marks[i])
                            .totalMarks(100.0)
                            .examType(types[i])
                            .term(Term.TERM_1)
                            .academicYear(2026)
                            .uploadedBy(1L)
                            .build());
                }
            }
            log.info("CSV data seeded successfully -232 students loaded");
        } catch (Exception ex){
            log.error("Failed to seed CSV data: {}",ex.getMessage());
        }
    }
}
