package com.elimupredict.common;

import com.elimupredict.user.UserRepository;
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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRecordRepository recordRepository;

    @Override
    public void run(String... args) throws Exception {
       seedUser("ADMIN001","System Admin","admin@123", Role.ADMIN);
       seedUser("ITH001","IT Handler","ithandler@123", Role.IT_HANDLER);

        seedUser("TCH001", "John Lagat", "teacher123", Role.TEACHER);
        seedUser("TCH002", "Jane Nekesa", "teacher123", Role.TEACHER);
        seedUser("SNR001", "James Okutoyi", "senior123", Role.SENIOR_TEACHER);
        seedUser("DEP001", "Aaron Mutua", "deputy123", Role.DEPUTY_PRINCIPAL);
        seedUser("PRI001", "Lovingstone Ochieng'", "principal123", Role.PRINCIPAL);
        seedUser("PAR001", "Herine Adhiambo", "parent123", Role.PARENT);

       seedCsvData();

//       Seed the seven subjects
        seedMultiSubjectData();
    }
    private  void seedUser(String username, String fullName, String password, Role role) {
        if (userRepository.existsByUsername(username)) {
            log.info("User {} already exists. Skipping seeding.", username);
            return;
        }

        userRepository.save(
                com.elimupredict.user.User.builder()
                        .username(username)
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

        Subject subject = subjectRepository.findBySubjectCode("GEO101")
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
                            .enrollmentYear(2026)
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

    private void seedMultiSubjectData() {

        // Check if multi-subject data already seeded
        if (subjectRepository.count() > 1) {
            log.info("[SEEDER] Multi-subject data already exists. Skipping.");
            return;
        }

        // Create all subjects first
        Subject math = getOrCreateSubject(
                "MATH101", "Mathematics", "Form 1N");
        Subject english = getOrCreateSubject(
                "ENG101", "English", "Form 1N");
        Subject biology = getOrCreateSubject(
                "BIO101", "Biology", "Form 1N");
        Subject chemistry = getOrCreateSubject(
                "CHEM101", "Chemistry", "Form 1N");
        Subject history = getOrCreateSubject(
                "HIST101", "History", "Form 1N");
        Subject physics = getOrCreateSubject(
                "PHY101", "Physics", "Form 1N");
        Subject computer = getOrCreateSubject(
                "COMP101", "Computer Studies", "Form 1N");

        log.info("[SEEDER] All subjects created. Seeding multi-subject data...");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                        getClass().getResourceAsStream(
                                "/data/multi_subject_data.csv"))))) {

            String line;
            boolean firstLine = true;
            int count = 0;

            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }

                String[] cols = line.split(",");


                String admissionNo = cols[0].trim();
                int subjectIdFromCsv = Integer.parseInt(cols[1].trim());
                // cols[2] is subject_name — skip, we use our DB subject

                double[] marks = {
                        Double.parseDouble(cols[3].trim()), // cat_1
                        Double.parseDouble(cols[4].trim()), // cat_2
                        Double.parseDouble(cols[5].trim()), // cat_3
                        Double.parseDouble(cols[6].trim()), // exam_1
                        Double.parseDouble(cols[7].trim())  // exam_2
                };

                // Map CSV subject_id to actual DB subject
                Subject subject = switch (subjectIdFromCsv) {
                    case 2 -> math;
                    case 3 -> english;
                    case 4 -> biology;
                    case 5 -> chemistry;
                    case 6 -> history;
                    case 7 -> physics;
                    case 8 -> computer;
                    default -> null;
                };

                if (subject == null) continue;

                // Student already registered from Geography seed
                // But register if somehow missing
                if (!studentRepository.existsByAdmissionNumber(admissionNo)) {
                    studentRepository.save(Student.builder()
                            .admissionNumber(admissionNo)
                            .fullName("Student " + admissionNo)
                            .className("Form 1N")
                            .enrollmentYear(2026)
                            .isActive(true)
                            .build());
                }

                ExamType[] types = {
                        ExamType.CAT_1, ExamType.CAT_2, ExamType.CAT_3,
                        ExamType.EXAM_1, ExamType.EXAM_2
                };

                for (int i = 0; i < marks.length; i++) {
                    // Avoid duplicates
                    if (!recordRepository
                            .existsByAdmissionNumberAndSubjectIdAndExamTypeAndTermAndAcademicYear(
                                    admissionNo, subject.getId(),
                                    types[i], Term.TERM_1, 2026)) {

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
                        count++;
                    }
                }
            }
            log.info("[SEEDER] Multi-subject data seeded — {} records loaded",
                    count);

        } catch (Exception ex) {
            log.error("[SEEDER] Failed to seed multi-subject data: {}",
                    ex.getMessage());
        }
    }

    private Subject getOrCreateSubject(
            String code, String name, String className) {
        return subjectRepository.findBySubjectCode(code)
                .orElseGet(() -> subjectRepository.save(Subject.builder()
                        .subjectCode(code)
                        .subjectName(name)
                        .className(className)
                        .isActive(true)
                        .build()));
    }
}
