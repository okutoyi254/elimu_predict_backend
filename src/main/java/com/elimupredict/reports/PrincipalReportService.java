package com.elimupredict.reports;

import com.elimupredict.ai.AIAnalysisRepository;
import com.elimupredict.common.enums.Term;
import com.elimupredict.marks.StudentRecordRepository;
import com.elimupredict.reports.dto.PrincipalDashboardDTO;
import com.elimupredict.student.StudentService;
import com.elimupredict.student.dto.StudentResponse;
import com.elimupredict.subject.SubjectRepository;
import com.elimupredict.subject.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


}
