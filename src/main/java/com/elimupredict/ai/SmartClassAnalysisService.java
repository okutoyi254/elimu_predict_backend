package com.elimupredict.ai;

import com.elimupredict.ai.dto.SmartClassInsightDTO;
import com.elimupredict.common.enums.Term;
import com.elimupredict.student.StudentService;
import com.elimupredict.student.dto.StudentResponse;
import com.elimupredict.subject.Subject;
import com.elimupredict.subject.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class SmartClassAnalysisService {

    private final AIAnalysisRepository analysisRepository;
    private final StudentService studentService;
    private final SubjectService subjectService;

    public SmartClassInsightDTO getSmartClassInsight(String className, Term term, Integer academicYear) {

        List<String> admNos = studentService.getStudentsByClassName(className)
                .stream().map(StudentResponse::getAdmissionNumber).toList();

        List<AiAnalysis> allAnalysis = analysisRepository
                .findByStudentsAndTerm(admNos, term, academicYear);

//        Group analysis by subject
        Map<Long, List<AiAnalysis>> bySubject = allAnalysis.stream()
                .collect(Collectors.groupingBy(AiAnalysis::getSubjectId));

        List<SmartClassInsightDTO.SubjectInsightDTO> subjectInsights =
                new ArrayList<>();

        for (Map.Entry<Long, List<AiAnalysis>> entry : bySubject.entrySet()) {

            Long subjectId = entry.getKey();
            List<AiAnalysis> analyses = entry.getValue();
            Subject subject = subjectService.getById(subjectId);

//        Standard class average
            double classAvgRisk = analyses.stream()
                    .filter(a -> a.getRiskLevel() != null)
                    .mapToDouble(AiAnalysis::getRiskPercentage)
                    .average().orElse(0.0);

//            High risk students in this subject
            List<AiAnalysis> highRisk = analyses.stream()
                    .filter(a -> "HIGH".equalsIgnoreCase(a.getRiskLevel()))
                    .toList();

            List<AiAnalysis> medRisk = analyses.stream()
                    .filter(a -> "MEDIUM".equalsIgnoreCase(a.getRiskLevel()))
                    .toList();


//        Smart insight. Hidden strugglers in a particular subject.
            List<SmartClassInsightDTO.HiddenWeakStudentDTO> hiddenStrugglers = new ArrayList<>();

            if (classAvgRisk < 40.0) {

                hiddenStrugglers = analyses.stream()
                        .filter(a -> a.getRiskLevel().equals("HIGH")

                                || (a.getRiskPercentage() != null && a.getRiskPercentage() > classAvgRisk + 25))
                        .filter(a -> a.getRiskPercentage() > classAvgRisk)
                        .map(a -> {
                            var student = studentService.findOrThrow(a.getAdmissionNumber());
                            return SmartClassInsightDTO.HiddenWeakStudentDTO
                                    .builder()
                                    .admissionNumber(a.getAdmissionNumber())
                                    .fullName(student.getFullName())
                                    .riskPercentage(a.getRiskPercentage())
                                    .riskLevel(a.getRiskLevel())
                                    .deviationFromTheClassAvg(a.getRiskPercentage() - classAvgRisk)
                                    .suggestion(a.getSuggestion())
                                    .urgency("CRITICAL — performing significantly " +
                                            "below class average in " +
                                            subject.getSubjectName()).build();
                        })
                        .sorted(Comparator.comparingDouble(SmartClassInsightDTO.HiddenWeakStudentDTO
                                ::getDeviationFromTheClassAvg).reversed())
                        .toList();
            }

//            Subject performance classification
            String subjectStatus;
            String actionRequired;

            if (classAvgRisk >= 70) {
                subjectStatus = "CRITICAL";
                actionRequired = "Immediate intervention required since  " +
                        "majority of class is at high risk";
            } else if (classAvgRisk >= 50) {
                subjectStatus = "CONCERNING";
                actionRequired = "Schedule targeted revision sessions and remedial sessions";
            } else if (classAvgRisk >= 30) {
                subjectStatus = "MODERATE";
                actionRequired = "Monitor closely and support weak students";
            } else {
                subjectStatus = "HEALTHY";
                actionRequired = hiddenStrugglers.isEmpty()
                        ? "Maintain current teaching approach"
                        : "Class performing well but " +
                        hiddenStrugglers.size() +
                        " student(s) need individual attention";
            }

//            Students whose risk is different from their peers

            double stdDev = calculateStdDev(analyses);
            List<String> outliersAbove = analyses.stream()
                    .filter(a -> a.getRiskPercentage() != null
                            && a.getRiskPercentage() > classAvgRisk - (2 * stdDev))
                    .map(AiAnalysis::getAdmissionNumber)
                    .toList();

            List<String> outliersBelow = analyses.stream()
                    .filter(a -> a.getRiskPercentage() != null
                            && a.getRiskPercentage() < classAvgRisk - (2 * stdDev))
                    .map(AiAnalysis::getAdmissionNumber)
                    .toList();

            subjectInsights.add(SmartClassInsightDTO.SubjectInsightDTO.builder()
                    .subjectId(subjectId)
                    .subjectName(subject.getSubjectName())
                    .classAverageRisk(Math.round(classAvgRisk * 10.0) / 10.0)
                    .subjectStatus(subjectStatus)
                    .actionRequired(actionRequired)
                    .highRiskCount(highRisk.size())
                    .mediumRiskCount(medRisk.size())
                    .lowRiskCount((int)(analyses.size()
                            - highRisk.size() - medRisk.size()))
                    .hiddenStrugglers(hiddenStrugglers)
                    .hiddenStrugglerCount(hiddenStrugglers.size())
                    .standardDeviation(Math.round(stdDev * 10.0) / 10.0)
                    .performanceOutliersHigh(outliersAbove)
                    .performanceOutliersLow(outliersBelow)
                    .build());

        }

        // Sort by class average risk descending — worst subjects first
        subjectInsights.sort(Comparator.comparingDouble(
                        SmartClassInsightDTO.SubjectInsightDTO::getClassAverageRisk)
                .reversed());

//        Students weak in multiple subjects
        List<SmartClassInsightDTO.MultiSubjectRiskDTO> multiSubjectRisk =
                findMultiSubjectRiskStudents(admNos, allAnalysis);

//        Students strong in some subjects but week in others.
        List<SmartClassInsightDTO.MixedPerformerDTO> mixedPerformers =
                findMixedPerformers(admNos, allAnalysis);

        return SmartClassInsightDTO.builder()
                .className(className)
                .totalStudents(admNos.size())
                .term(term.name())
                .academicYear(academicYear)
                .subjectInsights(subjectInsights)
                .multiSubjectRiskStudents(multiSubjectRisk)
                .mixedPerformers(mixedPerformers)
                .criticalSubject(subjectInsights.isEmpty() ? null
                        : subjectInsights.getFirst().getSubjectName())
                .totalWeakStudents(subjectInsights.stream()
                        .mapToInt(SmartClassInsightDTO.SubjectInsightDTO
                                ::getHiddenStrugglerCount).sum())
                .build();
    }


    private double calculateStdDev(List<AiAnalysis> analyses){

        double avg = analyses.stream()
                .mapToDouble(AiAnalysis::getRiskPercentage)
                .average().orElse(0.0);

        double variance = analyses.stream()
                .mapToDouble(a -> Math.pow(a.getRiskPercentage() - avg, 2))
                .average().orElse(0.0);

        return Math.sqrt(variance);
    }
}
