package com.elimupredict.reports;

import com.elimupredict.ai.AIAnalysisRepository;
import com.elimupredict.ai.AiAnalysis;
import com.elimupredict.common.enums.ExamType;
import com.elimupredict.common.enums.Term;
import com.elimupredict.marks.StudentRecord;
import com.elimupredict.marks.StudentRecordRepository;
import com.elimupredict.reports.dto.PrincipalDashboardDTO;
import com.elimupredict.reports.dto.SchoolAnalysisDTO;
import com.elimupredict.student.StudentService;
import com.elimupredict.student.dto.StudentResponse;
import com.elimupredict.subject.Subject;
import com.elimupredict.subject.SubjectRepository;
import com.elimupredict.subject.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrincipalReportService {

    private final StudentService studentService;
    private final StudentRecordRepository recordRepository;
    private final AIAnalysisRepository analysisRepository;
    private final SubjectService subjectService;
    private final SubjectRepository subjectRepository;
    private final RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public PrincipalDashboardDTO getPrincipalDashboard(
            Term term, Integer academicYear
    ){

        List<String> allClasses = studentService.getAvailableClasses();
        if(allClasses.isEmpty()){
            throw new RuntimeException("No classes found in the system");
        }

//        Previous term
        Term prevTerm = term == Term.TERM_1 ? Term.TERM_3
                : term == Term.TERM_2 ? Term.TERM_1 : Term.TERM_2;
        int prevYear = term == Term.TERM_1 ? academicYear - 1 : academicYear;

//        Build one graph per class
        List<PrincipalDashboardDTO.ClassGraphDTO> classGraphs =
                new ArrayList<>();

        Map<String, Double> classMeans = new HashMap<>();
        Map<String, Double> prevClassMeans = new HashMap<>();

        for (String className : allClasses) {
            List<String> admNos = studentService
                    .getStudentsByClassName(className)
                    .stream()
                    .map(s -> s.getAdmissionNumber())
                    .toList();

            double classMean = calculateClassMean(
                    admNos, term, academicYear);
            double prevMean = calculateClassMean(
                    admNos, prevTerm, prevYear);

            classMeans.put(className, classMean);
            prevClassMeans.put(className, prevMean);
        }
//School-wide mean for graph context lines
        double schoolMean = classMeans.values().stream()
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);

        double prevSchoolMean = prevClassMeans.values().stream()
                .filter(v -> v > 0)
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);

        double schoolMeanChange = prevSchoolMean > 0 ?
                schoolMean - prevSchoolMean : 0;

        String schoolTrend = Math.abs(schoolMeanChange) < 2 ? "STABLE"
                : schoolMeanChange > 0 ? "IMPROVING" : "DECLINING";

//        Class graphs
        List<Map.Entry<String, Double>> rankedClasses =
                classMeans.entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue()
                                .reversed())
                        .toList();

        for (int rank = 0; rank < rankedClasses.size(); rank++) {
            String className = rankedClasses.get(rank).getKey();
            double classMean = rankedClasses.get(rank).getValue();
            double prevMean = prevClassMeans.getOrDefault(className, 0.0);
            double change = prevMean > 0 ? classMean - prevMean : 0;
            String trend = Math.abs(change) < 2 ? "STABLE"
                    : change > 0 ? "IMPROVING" : "DECLINING";

            List<String> admNos = studentService
                    .getStudentsByClassName(className)
                    .stream()
                    .map(StudentResponse::getAdmissionNumber)
                    .toList();

//            Graph points for the class
            List<PrincipalDashboardDTO.GraphPointDTO> graphPoints =
                    buildClassGraphPoints(admNos, term,
                            academicYear, schoolMean);

            // Risk counts
            List<AiAnalysis> classAnalyses = analysisRepository
                    .findByStudentsAndTerm(admNos, term, academicYear);

            long high = classAnalyses.stream()
                    .filter(a -> "HIGH".equals(a.getRiskLevel())).count();
            long medium = classAnalyses.stream()
                    .filter(a -> "MEDIUM".equals(a.getRiskLevel())).count();
            long low = classAnalyses.stream()
                    .filter(a -> "LOW".equals(a.getRiskLevel())).count();

            String status = classMean < 40 ? "CRITICAL"
                    : classMean < 55 ? "CONCERNING"
                    : classMean < 65 ? "MODERATE" : "HEALTHY";

            String grade = classMean >= 75 ? "A"
                    : classMean >= 60 ? "B"
                    : classMean >= 50 ? "C"
                    : classMean >= 40 ? "D" : "E";

            classGraphs.add(PrincipalDashboardDTO.ClassGraphDTO.builder()
                    .className(className)
                    .totalStudents(admNos.size())
                    .classMeanAverage(Math.round(classMean * 10.0) / 10.0)
                    .classGrade(grade)
                    .classStatus(status)
                    .classRank(rank + 1)
                    .graphPoints(graphPoints)
                    .highRiskCount(high)
                    .mediumRiskCount(medium)
                    .lowRiskCount(low)
                    .previousTermMean(prevMean > 0 ?
                            Math.round(prevMean * 10.0) / 10.0 : null)
                    .meanChange(prevMean > 0 ?
                            Math.round(change * 10.0) / 10.0 : null)
                    .trend(trend)
                    .build());
        }

//        Class rankings
        List<PrincipalDashboardDTO.ClassRankDTO> classRankings =
                buildClassRankings(rankedClasses, classMeans,
                        prevClassMeans, term, academicYear);

//        Subject rankings
        List<PrincipalDashboardDTO.SubjectRankDTO> subjectRankings =
                buildSubjectRankings(allClasses, term,
                        academicYear, prevTerm, prevYear);

//        Most improved class
        PrincipalDashboardDTO.MostImprovedDTO mostImproved =
                findMostImprovedClass(classMeans, prevClassMeans);

//        Total students
        int totalStudents = allClasses.stream()
                .mapToInt(c -> studentService
                        .getStudentsByClassName(c).size())
                .sum();

        String schoolGrade = schoolMean >= 75 ? "A"
                : schoolMean >= 60 ? "B"
                : schoolMean >= 50 ? "C"
                : schoolMean >= 40 ? "D" : "E";

        return PrincipalDashboardDTO.builder()
                .term(term.name())
                .academicYear(academicYear)
                .totalStudents(totalStudents)
                .totalClasses(allClasses.size())
                .totalSubjects((int) subjectRepository.count())
                .schoolMeanAverage(Math.round(schoolMean * 10.0) / 10.0)
                .schoolGrade(schoolGrade)
                .previousTermMean(prevSchoolMean > 0 ?
                        Math.round(prevSchoolMean * 10.0) / 10.0 : null)
                .meanChange(prevSchoolMean > 0 ?
                        Math.round(schoolMeanChange * 10.0) / 10.0 : null)
                .schoolTrend(schoolTrend)
                .classGraphs(classGraphs)
                .classRankings(classRankings)
                .subjectRankings(subjectRankings)
                .mostImprovedClass(mostImproved)
                .build();

    }

public SchoolAnalysisDTO getSchoolAnalysis(
        Term term,Integer academicYear
) {

    List<String> allClasses = studentService.getAvailableClasses();

//        Build prompt summary
    StringBuilder summary = new StringBuilder();
    summary.append(String.format("School Performance Summary -%s %d\n\n",
            term.name().replace("_", " "), academicYear
    ));

    // Class performance
    summary.append("CLASS PERFORMANCE:\n");
    for (String className : allClasses) {
        List<String> admNos = studentService
                .getStudentsByClassName(className)
                .stream()
                .map(StudentResponse::getAdmissionNumber)
                .toList();

        double mean = calculateClassMean(admNos, term, academicYear);

        List<AiAnalysis> analyses = analysisRepository
                .findByStudentsAndTerm(admNos, term, academicYear);

        long high = analyses.stream()
                .filter(a -> "HIGH".equals(a.getRiskLevel())).count();

        summary.append(String.format(
                "- %s: Mean=%.1f, High Risk Students=%d/%d\n",
                className, mean, high, admNos.size()));
    }

    // Subject performance
    summary.append("\nSUBJECT PERFORMANCE (school-wide):\n");
    List<Subject> subjects = subjectRepository.findAll();

    for (Subject subject : subjects) {
        double subjectMean = allClasses.stream()
                .flatMap(c -> studentService
                        .getStudentsByClassName(c).stream()
                        .map(StudentResponse::getAdmissionNumber))
                .mapToDouble(admNo ->
                        recordRepository
                                .findByAdmissionNumberAndSubjectId(
                                        admNo, subject.getId())
                                .stream()
                                .filter(r -> r.getTerm() == term
                                        && r.getAcademicYear().equals(academicYear))
                                .mapToDouble(StudentRecord::getMarksObtained)
                                .average().orElse(0.0))
                .filter(m -> m > 0)
                .average().orElse(0.0);

        if (subjectMean > 0) {
            summary.append(String.format(
                    "- %s: School Mean=%.1f\n",
                    subject.getSubjectName(), subjectMean));
        }
    }

    // Build Gemini prompt
    String prompt = String.format(
            "You are an educational analyst. Analyze this school's " +
                    "performance data and provide:\n" +
                    "1. A 3-sentence overall analysis of school performance\n" +
                    "2. Top 3 areas needing urgent attention with specific " +
                    "recommendations\n" +
                    "3. Top 2 areas performing well\n" +
                    "4. A prioritized action plan with 5 specific steps\n" +
                    "5. Direct recommendation to the principal\n\n" +
                    "Format your response as JSON with these keys:\n" +
                    "overallAnalysis, areasNeedingAttention (array of " +
                    "{area, reason, priority, recommendation}), " +
                    "areasPerformingWell (array of {area, reason}), " +
                    "actionPlan (array of {priority, action, targetClass, " +
                    "targetSubject, expectedOutcome}), " +
                    "principalRecommendation\n\n" +
                    "Data:\n%s", summary.toString());

    try {
        String geminiResponse = callGemini(prompt);
        return parseGeminiAnalysis(geminiResponse, term, academicYear);
    } catch (Exception e) {
        log.error("[PRINCIPAL ANALYSIS] Gemini failed: {}", e.getMessage());
        return buildFallbackAnalysis(term, academicYear, allClasses);
    }
}

    private List<PrincipalDashboardDTO.GraphPointDTO> buildClassGraphPoints(
            List<String> admNos, Term term,
            Integer academicYear, double schoolMean) {

        List<ExamType> order = List.of(
                ExamType.CAT_1, ExamType.CAT_2, ExamType.CAT_3,
                ExamType.EXAM_1, ExamType.EXAM_2
        );

        Map<ExamType, String> labels = Map.of(
                ExamType.CAT_1,  "CAT 1",
                ExamType.CAT_2,  "CAT 2",
                ExamType.CAT_3,  "CAT 3",
                ExamType.EXAM_1, "EXAM 1",
                ExamType.EXAM_2, "EXAM 2"
        );

        return order.stream().map(examType -> {
            // For each student — get their average mark for this exam type
            // across ALL subjects they did
            double classAvg = admNos.stream()
                    .mapToDouble(admNo -> {
                        // All records for this student, this exam type, this term
                        List<StudentRecord> records = recordRepository
                                .findByAdmissionNumberAndTerm(admNo, term)
                                .stream()
                                .filter(r -> r.getAcademicYear().equals(academicYear)
                                        && r.getExamType() == examType)
                                .toList();

                        // Average across all subjects for this exam type
                        return records.stream()
                                .mapToDouble(StudentRecord::getMarksObtained)
                                .average().orElse(0.0);
                    })
                    .filter(m -> m > 0)
                    .average().orElse(0.0);

            return PrincipalDashboardDTO.GraphPointDTO.builder()
                    .label(labels.get(examType))
                    .classAverage(Math.round(classAvg * 10.0) / 10.0)
                    .schoolAverage(Math.round(schoolMean * 10.0) / 10.0)
                    .build();
        }).toList();
    }

    // Class rankings
    private List<PrincipalDashboardDTO.ClassRankDTO> buildClassRankings(
            List<Map.Entry<String, Double>> rankedClasses,
            Map<String, Double> classMeans,
            Map<String, Double> prevClassMeans,
            Term term, Integer academicYear) {

        List<PrincipalDashboardDTO.ClassRankDTO> rankings = new ArrayList<>();

        for (int i = 0; i < rankedClasses.size(); i++) {
            String className = rankedClasses.get(i).getKey();
            double mean = rankedClasses.get(i).getValue();
            double prev = prevClassMeans.getOrDefault(className, 0.0);
            double change = prev > 0 ? mean - prev : 0;
            String trend = Math.abs(change) < 2 ? "STABLE"
                    : change > 0 ? "IMPROVING" : "DECLINING";

            String grade = mean >= 75 ? "A"
                    : mean >= 60 ? "B"
                    : mean >= 50 ? "C"
                    : mean >= 40 ? "D" : "E";

            List<String> admNos = studentService
                    .getStudentsByClassName(className)
                    .stream()
                    .map(s -> s.getAdmissionNumber())
                    .toList();

            List<AiAnalysis> analyses = analysisRepository
                    .findByStudentsAndTerm(admNos, term, academicYear);

            long high = analyses.stream()
                    .filter(a -> "HIGH".equals(a.getRiskLevel())).count();

            // Best and worst subject for this class
            Map<Long, Double> subjectMeans = new HashMap<>();
            for (AiAnalysis a : analyses) {
                subjectMeans.merge(a.getSubjectId(),
                        a.getRiskPercentage() != null ?
                                a.getRiskPercentage() : 0.0,
                        Double::sum);
            }

            String bestSubject = "N/A", worstSubject = "N/A";
            if (!subjectMeans.isEmpty()) {
                Long bestId = Collections.min(subjectMeans.entrySet(),
                        Map.Entry.comparingByValue()).getKey();
                Long worstId = Collections.max(subjectMeans.entrySet(),
                        Map.Entry.comparingByValue()).getKey();
                try {
                    bestSubject = subjectService.getById(bestId).getSubjectName();
                    worstSubject = subjectService.getById(worstId).getSubjectName();
                } catch (Exception ignored) {}
            }

            rankings.add(PrincipalDashboardDTO.ClassRankDTO.builder()
                    .rank(i + 1)
                    .className(className)
                    .meanAverage(Math.round(mean * 10.0) / 10.0)
                    .grade(grade)
                    .trend(trend)
                    .meanChange(prev > 0 ?
                            Math.round(change * 10.0) / 10.0 : null)
                    .highRiskCount(high)
                    .totalStudents(admNos.size())
                    .bestSubject(bestSubject)
                    .worstSubject(worstSubject)
                    .build());
        }

        return rankings;
    }

    // Subject rankings school-wide
    private List<PrincipalDashboardDTO.SubjectRankDTO> buildSubjectRankings(
            List<String> allClasses, Term term, Integer academicYear,
            Term prevTerm, Integer prevYear) {

        List<Subject> subjects = subjectRepository.findAll();
        List<PrincipalDashboardDTO.SubjectRankDTO> rankings = new ArrayList<>();

        for (Subject subject : subjects) {
            // All students across all classes
            List<String> allAdmNos = allClasses.stream()
                    .flatMap(c -> studentService
                            .getStudentsByClassName(c).stream()
                            .map(s -> s.getAdmissionNumber()))
                    .toList();

            double mean = allAdmNos.stream()
                    .mapToDouble(admNo ->
                            recordRepository
                                    .findByAdmissionNumberAndSubjectId(
                                            admNo, subject.getId())
                                    .stream()
                                    .filter(r -> r.getTerm() == term
                                            && r.getAcademicYear().equals(academicYear))
                                    .mapToDouble(StudentRecord::getMarksObtained)
                                    .average().orElse(0.0))
                    .filter(m -> m > 0)
                    .average().orElse(0.0);

            if (mean == 0.0) continue; // Skip subjects with no data

            double prevMean = allAdmNos.stream()
                    .mapToDouble(admNo ->
                            recordRepository
                                    .findByAdmissionNumberAndSubjectId(
                                            admNo, subject.getId())
                                    .stream()
                                    .filter(r -> r.getTerm() == prevTerm
                                            && r.getAcademicYear().equals(prevYear))
                                    .mapToDouble(StudentRecord::getMarksObtained)
                                    .average().orElse(0.0))
                    .filter(m -> m > 0)
                    .average().orElse(0.0);

            double change = prevMean > 0 ? mean - prevMean : 0;
            String trend = Math.abs(change) < 2 ? "STABLE"
                    : change > 0 ? "IMPROVING" : "DECLINING";
            String grade = mean >= 75 ? "A"
                    : mean >= 60 ? "B"
                    : mean >= 50 ? "C"
                    : mean >= 40 ? "D" : "E";
            String status = mean < 40 ? "CRITICAL"
                    : mean < 55 ? "CONCERNING"
                    : mean < 65 ? "MODERATE" : "HEALTHY";

            // High risk count for this subject
            long highRisk = analysisRepository
                    .findByStudentsAndTerm(allAdmNos, term, academicYear)
                    .stream()
                    .filter(a -> a.getSubjectId().equals(subject.getId())
                            && "HIGH".equals(a.getRiskLevel()))
                    .count();

            rankings.add(PrincipalDashboardDTO.SubjectRankDTO.builder()
                    .subjectName(subject.getSubjectName())
                    .subjectCode(subject.getSubjectCode())
                    .schoolWideMean(Math.round(mean * 10.0) / 10.0)
                    .grade(grade)
                    .trend(trend)
                    .meanChange(prevMean > 0 ?
                            Math.round(change * 10.0) / 10.0 : null)
                    .highRiskCount(highRisk)
                    .totalStudents((long) allAdmNos.size())
                    .status(status)
                    .build());
        }

        // Rank by mean descending
        rankings.sort(Comparator.comparingDouble(
                        PrincipalDashboardDTO.SubjectRankDTO::getSchoolWideMean)
                .reversed());

        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }

        return rankings;
    }

    // Most improved class
    private PrincipalDashboardDTO.MostImprovedDTO findMostImprovedClass(
            Map<String, Double> currentMeans,
            Map<String, Double> prevMeans) {

        String bestClass = null;
        double bestImprovement = Double.MIN_VALUE;

        for (Map.Entry<String, Double> entry : currentMeans.entrySet()) {
            String className = entry.getKey();
            double prev = prevMeans.getOrDefault(className, 0.0);
            if (prev <= 0) continue;

            double improvement = entry.getValue() - prev;
            if (improvement > bestImprovement) {
                bestImprovement = improvement;
                bestClass = className;
            }
        }

        if (bestClass == null) {
            return PrincipalDashboardDTO.MostImprovedDTO.builder()
                    .message("No previous term data available for comparison.")
                    .build();
        }

        double current = currentMeans.get(bestClass);
        double prev = prevMeans.get(bestClass);
        double pctImprovement = prev > 0 ?
                (bestImprovement / prev) * 100 : 0;

        return PrincipalDashboardDTO.MostImprovedDTO.builder()
                .className(bestClass)
                .previousMean(Math.round(prev * 10.0) / 10.0)
                .currentMean(Math.round(current * 10.0) / 10.0)
                .improvement(Math.round(bestImprovement * 10.0) / 10.0)
                .improvementPercentage(
                        String.format("+%.1f%%", pctImprovement))
                .message(String.format(
                        "%s showed the greatest improvement this term — " +
                                "mean average rose from %.1f to %.1f (+%.1f marks). " +
                                "Commend the teachers and students in this class.",
                        bestClass, prev, current, bestImprovement))
                .build();
    }


    // Gemini call service logic
    @SuppressWarnings("unchecked")
    private String callGemini(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                geminiApiUrl + "?key=" + geminiApiKey,
                new HttpEntity<>(body, headers),
                Map.class
        );

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.getBody().get("candidates");
        Map<String, Object> content =
                (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.get("parts");
        return (String) parts.get(0).get("text");
    }

    @SuppressWarnings("unchecked")
    private SchoolAnalysisDTO parseGeminiAnalysis(
            String raw, Term term, Integer academicYear) {
        try {
            // Strip markdown code fences if present
            String cleaned = raw
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> parsed = mapper.readValue(cleaned, Map.class);

            // Parse areas needing attention
            List<SchoolAnalysisDTO.FocusAreaDTO> needsAttention =
                    new ArrayList<>();
            List<Map<String, Object>> attention =
                    (List<Map<String, Object>>) parsed.get(
                            "areasNeedingAttention");
            if (attention != null) {
                for (Map<String, Object> a : attention) {
                    needsAttention.add(SchoolAnalysisDTO.FocusAreaDTO.builder()
                            .area(str(a, "area"))
                            .reason(str(a, "reason"))
                            .priority(str(a, "priority"))
                            .recommendation(str(a, "recommendation"))
                            .build());
                }
            }

            // Parse areas performing well
            List<SchoolAnalysisDTO.FocusAreaDTO> performingWell =
                    new ArrayList<>();
            List<Map<String, Object>> well =
                    (List<Map<String, Object>>) parsed.get(
                            "areasPerformingWell");
            if (well != null) {
                for (Map<String, Object> w : well) {
                    performingWell.add(SchoolAnalysisDTO.FocusAreaDTO.builder()
                            .area(str(w, "area"))
                            .reason(str(w, "reason"))
                            .build());
                }
            }

            // Parse action plan
            List<SchoolAnalysisDTO.ActionItemDTO> actionPlan =
                    new ArrayList<>();
            List<Map<String, Object>> actions =
                    (List<Map<String, Object>>) parsed.get("actionPlan");
            if (actions != null) {
                for (Map<String, Object> ac : actions) {
                    actionPlan.add(SchoolAnalysisDTO.ActionItemDTO.builder()
                            .priority(((Number) ac.getOrDefault(
                                    "priority", 0)).intValue())
                            .action(str(ac, "action"))
                            .targetClass(str(ac, "targetClass"))
                            .targetSubject(str(ac, "targetSubject"))
                            .expectedOutcome(str(ac, "expectedOutcome"))
                            .build());
                }
            }

            return SchoolAnalysisDTO.builder()
                    .term(term.name())
                    .academicYear(academicYear)
                    .generatedAt(LocalDateTime.now().toString())
                    .overallAnalysis(str(parsed, "overallAnalysis"))
                    .principalRecommendation(
                            str(parsed, "principalRecommendation"))
                    .areasNeedingAttention(needsAttention)
                    .areasPerformingWell(performingWell)
                    .actionPlan(actionPlan)
                    .build();

        } catch (Exception e) {
            log.error("[PRINCIPAL] Failed to parse Gemini response: {}",
                    e.getMessage());
            return buildFallbackAnalysis(term, academicYear, List.of());
        }
    }

    private SchoolAnalysisDTO buildFallbackAnalysis(
            Term term, Integer academicYear, List<String> classes) {
        return SchoolAnalysisDTO.builder()
                .term(term.name())
                .academicYear(academicYear)
                .generatedAt(LocalDateTime.now().toString())
                .overallAnalysis(
                        "School performance analysis is currently unavailable. " +
                                "Please ensure all classes have been analyzed and " +
                                "try again.")
                .principalRecommendation(
                        "Ensure all teachers have run analysis for their " +
                                "classes before requesting the school analysis.")
                .areasNeedingAttention(List.of())
                .areasPerformingWell(List.of())
                .actionPlan(List.of())
                .build();
    }


    private double calculateClassMean(
            List<String> admNos, Term term, Integer academicYear) {
        return admNos.stream()
                .mapToDouble(admNo ->
                        recordRepository.findByAdmissionNumberAndTerm(admNo, term)
                                .stream()
                                .filter(r -> r.getAcademicYear().equals(academicYear))
                                .mapToDouble(StudentRecord::getMarksObtained)
                                .average().orElse(0.0))
                .filter(m -> m > 0)
                .average().orElse(0.0);
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}


