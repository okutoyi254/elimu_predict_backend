package com.elimupredict.reports;

import com.elimupredict.ai.AIAnalysisRepository;
import com.elimupredict.ai.AiAnalysis;
import com.elimupredict.ai.GeminiService;
import com.elimupredict.common.enums.ExamType;
import com.elimupredict.common.enums.Term;
import com.elimupredict.marks.StudentRecord;
import com.elimupredict.marks.StudentRecordRepository;
import com.elimupredict.reports.dto.ParentStudentProfileDTO;
import com.elimupredict.reports.dto.SuggestionTabDTO;
import com.elimupredict.student.Student;
import com.elimupredict.student.StudentService;
import com.elimupredict.subject.Subject;
import com.elimupredict.subject.SubjectService;
import com.elimupredict.user.User;
import com.elimupredict.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParentReportService {

    private final StudentRecordRepository recordRepository;
    private final AIAnalysisRepository analysisRepository;
    private final StudentService studentService;
    private final SubjectService subjectService;
    private final UserRepository userRepository;
    private final GeminiService geminiService;

//    landing page
    public List<ParentStudentProfileDTO> getChildrenProfiles(
            String parentUserId, Term term, Integer academicYear) {

        User parent = userRepository.findByUsername(parentUserId)
                .orElseThrow(() -> new RuntimeException(
                        "Parent not found: " + parentUserId));

        List<Student> children = studentService
                .getByParentId(parent.getId())
                .stream()
                .map(s -> studentService.findOrThrow(s.getAdmissionNumber()))
                .toList();

        if (children.isEmpty()) {
            throw new RuntimeException(
                    "No students linked to your account. " +
                            "Contact the IT Handler to link your children.");
        }

        return children.stream()
                .map(child -> buildProfile(child, term, academicYear))
                .toList();
    }

//Build child profile
    public ParentStudentProfileDTO getChildFullProfile(
            String parentUserId, String admissionNumber,
            Term term, Integer academicYear) {

        // Verify child belongs to parent
        User parent = userRepository.findByUsername(parentUserId)
                .orElseThrow(() -> new RuntimeException(
                        "Parent not found: " + parentUserId));

        boolean isParentsChild = studentService
                .getByParentId(parent.getId())
                .stream()
                .anyMatch(s -> s.getAdmissionNumber().equals(admissionNumber));

        if (!isParentsChild) {
            throw new RuntimeException(
                    "Student " + admissionNumber +
                            " is not linked to your account.");
        }

        Student child = studentService.findOrThrow(admissionNumber);
        return buildProfile(child, term, academicYear);
    }

//    Student improvement suggestion
    public SuggestionTabDTO getSuggestions(
            String parentUserId, String admissionNumber,
            Term term, Integer academicYear) {

        User parent = userRepository.findByUsername(parentUserId)
                .orElseThrow(() -> new RuntimeException(
                        "Parent not found: " + parentUserId));

        boolean isParentsChild = studentService
                .getByParentId(parent.getId())
                .stream()
                .anyMatch(s -> s.getAdmissionNumber().equals(admissionNumber));

        if (!isParentsChild) {
            throw new RuntimeException("Access denied.");
        }

        Student child = studentService.findOrThrow(admissionNumber);

        List<AiAnalysis> analyses = analysisRepository
                .findByAdmissionNumberAndTermAndAcademicYear(
                        admissionNumber, term, academicYear);

        if (analyses.isEmpty()) {
            throw new RuntimeException(
                    "No analysis data found. " +
                            "Please ask the teacher to run analysis first.");
        }

        List<SuggestionTabDTO.SubjectSuggestionDTO> suggestions =
                new ArrayList<>();

        int needsAttention = 0;
        int performingWell = 0;

        for (AiAnalysis analysis : analyses) {
            Subject subject = subjectService.getById(analysis.getSubjectId());

            double currentMean = recordRepository
                    .findByAdmissionNumberAndSubjectId(
                            admissionNumber, analysis.getSubjectId())
                    .stream()
                    .filter(r -> r.getTerm() == term
                            && r.getAcademicYear().equals(academicYear))
                    .mapToDouble(StudentRecord::getMarksObtained)
                    .average().orElse(0.0);

            String suggestion = analysis.getSuggestion();
            boolean freshlyGenerated = false;


            if (("HIGH".equals(analysis.getRiskLevel())
                    || "MEDIUM".equals(analysis.getRiskLevel()))) {

                needsAttention++;

                if (suggestion == null || suggestion.isBlank()) {
                    List<Double> marks = recordRepository
                            .findByAdmissionNumberAndSubjectId(
                                    admissionNumber, analysis.getSubjectId())
                            .stream()
                            .filter(r -> r.getTerm() == term
                                    && r.getAcademicYear().equals(academicYear))
                            .sorted(Comparator.comparing(
                                    r -> r.getExamType().ordinal()))
                            .map(StudentRecord::getMarksObtained)
                            .toList();

                    log.info("[PARENT SUGGESTION] Generating fresh Gemini " +
                                    "suggestion for {} — {}",
                            admissionNumber, subject.getSubjectName());

                    suggestion = geminiService.generateSuggestion(
                            subject.getSubjectName(),
                            analysis.getRiskPercentage() != null ?
                                    analysis.getRiskPercentage() : 60.0,
                            marks);
                    freshlyGenerated = true;

                    // Save back
                    analysis.setSuggestion(suggestion);
                    analysisRepository.save(analysis);
                }

            } else {
                performingWell++;
            }

            List<String> actionPoints = parseActionPoints(suggestion);

            suggestions.add(SuggestionTabDTO.SubjectSuggestionDTO.builder()
                    .subjectName(subject.getSubjectName())
                    .riskLevel(analysis.getRiskLevel())
                    .riskPercentage(analysis.getRiskPercentage())
                    .currentMean(Math.round(currentMean * 10.0) / 10.0)
                    .suggestion(suggestion)
                    .freshlyGenerated(freshlyGenerated)
                    .actionPoints(actionPoints)
                    .build());
        }

        suggestions.sort(Comparator.comparing(s -> {
            return switch (s.getRiskLevel()) {
                case "HIGH" -> 0;
                case "MEDIUM" -> 1;
                default -> 2;
            };
        }));

        String overallMessage = buildOverallSuggestionMessage(
                child.getFullName(), needsAttention,
                suggestions.size());

        return SuggestionTabDTO.builder()
                .admissionNumber(admissionNumber)
                .fullName(child.getFullName())
                .term(term.name())
                .academicYear(academicYear)
                .overallMessage(overallMessage)
                .subjectSuggestions(suggestions)
                .subjectsNeedingAttention(needsAttention)
                .subjectsPerformingWell(performingWell)
                .build();
    }


//    Student profile tabs
    private ParentStudentProfileDTO buildProfile(
            Student child, Term term, Integer academicYear) {

        String admNo = child.getAdmissionNumber();
        String className = child.getClassName();

        List<AiAnalysis> analyses = analysisRepository
                .findByAdmissionNumberAndTermAndAcademicYear(
                        admNo, term, academicYear);

        List<StudentRecord> allRecords = recordRepository
                .findByAdmissionNumberAndTerm(admNo, term)
                .stream()
                .filter(r -> r.getAcademicYear().equals(academicYear))
                .toList();

        // Previous term
        Term prevTerm = term == Term.TERM_1 ? Term.TERM_3
                : term == Term.TERM_2 ? Term.TERM_1 : Term.TERM_2;
        int prevYear = term == Term.TERM_1 ? academicYear - 1 : academicYear;

        // Class students for ranking
        List<String> classAdmNos = studentService
                .getStudentsByClassName(className)
                .stream()
                .map(s -> s.getAdmissionNumber())
                .toList();

        // Class means for ranking
        Map<String, Double> classMeans = new HashMap<>();
        for (String admNo2 : classAdmNos) {
            double mean = recordRepository
                    .findByAdmissionNumberAndTerm(admNo2, term)
                    .stream()
                    .filter(r -> r.getAcademicYear().equals(academicYear))
                    .mapToDouble(StudentRecord::getMarksObtained)
                    .average().orElse(0.0);
            classMeans.put(admNo2, mean);
        }

        List<Map.Entry<String, Double>> ranked = classMeans.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .toList();

        int position = 1;
        for (Map.Entry<String, Double> e : ranked) {
            if (e.getKey().equals(admNo)) break;
            position++;
        }

        int total = classAdmNos.size();
        double percentile = ((total - position) / (double) total) * 100;

        // Overall marks
        double totalObtained = allRecords.stream()
                .mapToDouble(StudentRecord::getMarksObtained).sum();
        double totalAvailable = allRecords.stream()
                .mapToDouble(StudentRecord::getTotalMarks).sum();
        double overallPct = totalAvailable > 0 ?
                (totalObtained / totalAvailable) * 100 : 0;
        double meanAvg = allRecords.stream()
                .mapToDouble(StudentRecord::getMarksObtained)
                .average().orElse(0.0);

        String grade = meanAvg >= 75 ? "A"
                : meanAvg >= 60 ? "B"
                : meanAvg >= 50 ? "C"
                : meanAvg >= 40 ? "D" : "E";

        String overallRisk = analyses.isEmpty() ? "NOT_ANALYZED"
                : determineOverallRisk(analyses);

        double avgRisk = analyses.stream()
                .filter(a -> a.getRiskPercentage() != null)
                .mapToDouble(AiAnalysis::getRiskPercentage)
                .average().orElse(0.0);

        // Trajectory
        double prevMean = recordRepository
                .findByAdmissionNumberAndTerm(admNo, prevTerm)
                .stream()
                .filter(r -> r.getAcademicYear().equals(prevYear))
                .mapToDouble(StudentRecord::getMarksObtained)
                .average().orElse(0.0);

        double meanChange = prevMean > 0 ? meanAvg - prevMean : 0;
        String trajectory = Math.abs(meanChange) < 2 ? "STABLE"
                : meanChange > 0 ? "IMPROVING" : "DECLINING";

        String band = percentile >= 90 ? "TOP 10%"
                : percentile >= 50 ? "UPPER HALF"
                : percentile >= 10 ? "LOWER HALF"
                : "BOTTOM 10%";

        return ParentStudentProfileDTO.builder()
                .admissionNumber(admNo)
                .fullName(child.getFullName())
                .className(className)
                .term(term.name())
                .academicYear(academicYear)
                .performance(buildPerformanceTab(admNo, analyses, allRecords,
                        classAdmNos, position, total, percentile, band,
                        meanAvg, totalObtained, totalAvailable, overallPct,
                        grade, term, academicYear))
                .analysis(buildAnalysisTab(admNo, analyses, avgRisk,
                        overallRisk, trajectory, meanAvg, prevMean,
                        meanChange, prevTerm, prevYear, child.getFullName()))
                .assessmentBreakDown(buildAssessmentTab(admNo,
                        allRecords, analyses, classAdmNos, term, academicYear))
                .build();
    }

//Performance for a single student
    private ParentStudentProfileDTO.PerformanceTabDTO buildPerformanceTab(
            String admNo,
            List<AiAnalysis> analyses,
            List<StudentRecord> allRecords,
            List<String> classAdmNos,
            int position, int total,
            double percentile, String band,
            double meanAvg, double totalObtained,
            double totalAvailable, double overallPct,
            String grade, Term term, Integer academicYear) {

        Map<Long, List<StudentRecord>> bySubject = allRecords.stream()
                .collect(Collectors.groupingBy(StudentRecord::getSubjectId));

        List<ParentStudentProfileDTO.PerformanceTabDTO.SubjectPerformanceRowDTO>
                rows = new ArrayList<>();

        String bestSubject = null, worstSubject = null;
        double bestMean = -1, worstMean = 101;

        for (Map.Entry<Long, List<StudentRecord>> entry : bySubject.entrySet()) {
            Subject subject = subjectService.getById(entry.getKey());
            List<StudentRecord> subRecords = entry.getValue();

            double mean = subRecords.stream()
                    .mapToDouble(StudentRecord::getMarksObtained)
                    .average().orElse(0.0);
            double subTotal = subRecords.stream()
                    .mapToDouble(StudentRecord::getMarksObtained).sum();
            double subMax = subRecords.stream()
                    .mapToDouble(StudentRecord::getTotalMarks).sum();
            double subPct = subMax > 0 ? (subTotal / subMax) * 100 : 0;

            String subGrade = mean >= 75 ? "A"
                    : mean >= 60 ? "B"
                    : mean >= 50 ? "C"
                    : mean >= 40 ? "D" : "E";

            // Class average for this subject
            double classSubAvg = classAdmNos.stream()
                    .mapToDouble(a -> recordRepository
                            .findByAdmissionNumberAndSubjectId(a, entry.getKey())
                            .stream()
                            .filter(r -> r.getTerm() == term
                                    && r.getAcademicYear().equals(academicYear))
                            .mapToDouble(StudentRecord::getMarksObtained)
                            .average().orElse(0.0))
                    .average().orElse(0.0);

            // Position in class for this subject
            int subPos = 1;
            for (String admNo2 : classAdmNos) {
                double otherMean = recordRepository
                        .findByAdmissionNumberAndSubjectId(admNo2, entry.getKey())
                        .stream()
                        .filter(r -> r.getTerm() == term
                                && r.getAcademicYear().equals(academicYear))
                        .mapToDouble(StudentRecord::getMarksObtained)
                        .average().orElse(0.0);
                if (otherMean > mean) subPos++;
            }

            double dev = mean - classSubAvg;
            String vsClass = dev > 5 ? "ABOVE AVERAGE"
                    : dev < -5 ? "BELOW AVERAGE" : "AVERAGE";

            if (mean > bestMean) { bestMean = mean; bestSubject = subject.getSubjectName(); }
            if (mean < worstMean) { worstMean = mean; worstSubject = subject.getSubjectName(); }

            rows.add(ParentStudentProfileDTO.PerformanceTabDTO
                    .SubjectPerformanceRowDTO.builder()
                    .subjectName(subject.getSubjectName())
                    .mean(Math.round(mean * 10.0) / 10.0)
                    .total(Math.round(subTotal * 10.0) / 10.0)
                    .percentage(Math.round(subPct * 10.0) / 10.0)
                    .grade(subGrade)
                    .positionInClass(subPos)
                    .performanceVsClass(vsClass)
                    .build());
        }

        rows.sort(Comparator.comparingDouble(
                ParentStudentProfileDTO.PerformanceTabDTO
                        .SubjectPerformanceRowDTO::getMean).reversed());

        return ParentStudentProfileDTO.PerformanceTabDTO.builder()
                .totalMarksObtained(Math.round(totalObtained * 10.0) / 10.0)
                .totalMarksAvailable(totalAvailable)
                .overallGrade(grade)
                .classPosition(position)
                .totalStudents(total)
                .performanceBand(band)
                .bestSubject(bestSubject)
                .bestSubjectMean(Math.round(bestMean * 10.0) / 10.0)
                .worstSubject(worstSubject)
                .worstSubjectMean(Math.round(worstMean * 10.0) / 10.0)
                .build();
    }

//General analysis
    private ParentStudentProfileDTO.AnalysisTabDTO buildAnalysisTab(
            String admNo,
            List<AiAnalysis> analyses,
            double avgRisk, String overallRisk,
            String trajectory, double currentMean,
            double prevMean, double meanChange,
            Term prevTerm, int prevYear,
            String fullName) {

        List<String> highRisk = new ArrayList<>();
        List<String> mediumRisk = new ArrayList<>();
        List<String> lowRisk = new ArrayList<>();

        int overallTrend = (int) analyses.stream()
                .filter(a -> a.getTrend() != null)
                .mapToInt(AiAnalysis::getTrend)
                .average().orElse(0);

        List<ParentStudentProfileDTO.AnalysisTabDTO.SubjectRiskSummaryDTO>
                subjectRiskSummaries = analyses.stream().map(a -> {
            Subject subject = subjectService.getById(a.getSubjectId());
            String subjectName = subject.getSubjectName();

            if ("HIGH".equals(a.getRiskLevel())) highRisk.add(subjectName);
            else if ("MEDIUM".equals(a.getRiskLevel())) mediumRisk.add(subjectName);
            else lowRisk.add(subjectName);

            // Previous term mean for this subject
            double prevSubMean = recordRepository
                    .findByAdmissionNumberAndSubjectId(admNo, a.getSubjectId())
                    .stream()
                    .filter(r -> r.getTerm() == prevTerm
                            && r.getAcademicYear() == prevYear)
                    .mapToDouble(StudentRecord::getMarksObtained)
                    .average().orElse(0.0);

            double currSubMean = recordRepository
                    .findByAdmissionNumberAndSubjectId(admNo, a.getSubjectId())
                    .stream()
                    .filter(r -> r.getTerm().name().equals(
                            analyses.getFirst().getTerm().name()))
                    .mapToDouble(StudentRecord::getMarksObtained)
                    .average().orElse(0.0);

            double subChange = prevSubMean > 0 ? currSubMean - prevSubMean : 0;
            String improvement = prevSubMean > 0 ?
                    (subChange >= 3 ? "IMPROVED" :
                            subChange <= -3 ? "DECLINED" : "STABLE") : "NO_DATA";

            String trendDir = a.getTrend() == null ? "STABLE"
                    : a.getTrend() > 3 ? "IMPROVING"
                    : a.getTrend() < -3 ? "DECLINING" : "STABLE";

            return ParentStudentProfileDTO.AnalysisTabDTO
                    .SubjectRiskSummaryDTO.builder()
                    .subjectName(subjectName)
                    .riskPercentage(a.getRiskPercentage())
                    .riskLevel(a.getRiskLevel())
                    .trend(a.getTrend())
                    .trendDirection(trendDir)
                    .improvement(improvement)
                    .previousTermMean(prevSubMean > 0 ?
                            Math.round(prevSubMean * 10.0) / 10.0 : null)
                    .currentTermMean(Math.round(currSubMean * 10.0) / 10.0)
                    .markChange(prevSubMean > 0 ?
                            Math.round(subChange * 10.0) / 10.0 : null)
                    .build();
        }).toList();

        String trajectoryMsg = buildTrajectoryMessage(
                fullName, trajectory, meanChange, prevMean,
                currentMean, prevTerm);

        String parentSummary = buildParentSummary(
                fullName, trajectory, overallRisk,
                highRisk, mediumRisk, lowRisk);

        return ParentStudentProfileDTO.AnalysisTabDTO.builder()
                .averageRiskScore(Math.round(avgRisk * 10.0) / 10.0)
                .overallRiskLevel(overallRisk)
                .trajectoryLabel(trajectory)
                .trajectoryMessage(trajectoryMsg)
                .overallTrend(overallTrend)
                .currentTermMean(Math.round(currentMean * 10.0) / 10.0)
                .previousTermMean(prevMean > 0 ?
                        Math.round(prevMean * 10.0) / 10.0 : null)
                .meanChange(prevMean > 0 ?
                        Math.round(meanChange * 10.0) / 10.0 : null)
                .previousTerm(prevTerm.name())
                .previousYear(prevYear)
                .hasComparison(prevMean > 0)
                .subjectRiskSummaries(subjectRiskSummaries)
                .highRiskSubjects(highRisk)
                .mediumRiskSubjects(mediumRisk)
                .lowRiskSubjects(lowRisk)
                .parentSummary(parentSummary)
                .build();
    }


//    Assessment breakdown
    private ParentStudentProfileDTO.AssessmentTabDTO buildAssessmentTab(
            String admNo,
            List<StudentRecord> allRecords,
            List<AiAnalysis> analyses,
            List<String> classAdmNos,
            Term term, Integer academicYear) {

        // Average per exam type across all subjects for this student
        double cat1Avg = averageByType(allRecords, ExamType.CAT_1);
        double cat2Avg = averageByType(allRecords, ExamType.CAT_2);
        double cat3Avg = averageByType(allRecords, ExamType.CAT_3);
        double exam1Avg = averageByType(allRecords, ExamType.EXAM_1);
        double exam2Avg = averageByType(allRecords, ExamType.EXAM_2);

        // Class averages per exam type for context
        double classCat1 = classAvgByType(classAdmNos, ExamType.CAT_1, term, academicYear);
        double classCat2 = classAvgByType(classAdmNos, ExamType.CAT_2, term, academicYear);
        double classCat3 = classAvgByType(classAdmNos, ExamType.CAT_3, term, academicYear);
        double classExam1 = classAvgByType(classAdmNos, ExamType.EXAM_1, term, academicYear);
        double classExam2 = classAvgByType(classAdmNos, ExamType.EXAM_2, term, academicYear);

        // Graph data — ready for recharts
        List<ParentStudentProfileDTO.AssessmentTabDTO.GraphPointDTO> graphData =
                List.of(
                        point("CAT 1", cat1Avg, classCat1),
                        point("CAT 2", cat2Avg, classCat2),
                        point("CAT 3", cat3Avg, classCat3),
                        point("EXAM 1", exam1Avg, classExam1),
                        point("EXAM 2", exam2Avg, classExam2)
                );

        // Per subject breakdown
        Map<Long, List<StudentRecord>> bySubject = allRecords.stream()
                .collect(Collectors.groupingBy(StudentRecord::getSubjectId));

        List<ParentStudentProfileDTO.AssessmentTabDTO.SubjectAssessmentDTO>
                subjectAssessments = bySubject.entrySet().stream().map(entry -> {
            Subject subject = subjectService.getById(entry.getKey());
            List<StudentRecord> recs = entry.getValue();

            Map<String, Double> marks = recs.stream()
                    .collect(Collectors.toMap(
                            r -> r.getExamType().name(),
                            StudentRecord::getMarksObtained,
                            (a, b) -> a));

            double mean = recs.stream()
                    .mapToDouble(StudentRecord::getMarksObtained)
                    .average().orElse(0.0);

            // Trend: compare first assessment to last
            List<Double> sorted = recs.stream()
                    .sorted(Comparator.comparing(r -> r.getExamType().ordinal()))
                    .map(StudentRecord::getMarksObtained)
                    .toList();

            String trend = "STABLE";
            if (sorted.size() >= 2) {
                double diff = sorted.get(sorted.size() - 1) - sorted.get(0);
                trend = diff > 5 ? "IMPROVING" : diff < -5 ? "DECLINING" : "STABLE";
            }

            return ParentStudentProfileDTO.AssessmentTabDTO
                    .SubjectAssessmentDTO.builder()
                    .subjectName(subject.getSubjectName())
                    .cat1(marks.get("CAT_1"))
                    .cat2(marks.get("CAT_2"))
                    .cat3(marks.get("CAT_3"))
                    .exam1(marks.get("EXAM_1"))
                    .exam2(marks.get("EXAM_2"))
                    .mean(Math.round(mean * 10.0) / 10.0)
                    .trend(trend)
                    .build();
        }).toList();

        return ParentStudentProfileDTO.AssessmentTabDTO.builder()
                .cat1Average(Math.round(cat1Avg * 10.0) / 10.0)
                .cat2Average(Math.round(cat2Avg * 10.0) / 10.0)
                .cat3Average(Math.round(cat3Avg * 10.0) / 10.0)
                .exam1Average(Math.round(exam1Avg * 10.0) / 10.0)
                .exam2Average(Math.round(exam2Avg * 10.0) / 10.0)
                .graphData(graphData)
                .subjectAssessments(subjectAssessments)
                .build();
    }

//Utility methods
    private double averageByType(
            List<StudentRecord> records, ExamType type) {
        return records.stream()
                .filter(r -> r.getExamType() == type)
                .mapToDouble(StudentRecord::getMarksObtained)
                .average().orElse(0.0);
    }

    private double classAvgByType(
            List<String> classAdmNos, ExamType type,
            Term term, Integer academicYear) {
        return classAdmNos.stream()
                .mapToDouble(admNo ->
                        recordRepository.findByAdmissionNumberAndTerm(admNo, term)
                                .stream()
                                .filter(r -> r.getAcademicYear().equals(academicYear)
                                        && r.getExamType() == type)
                                .mapToDouble(StudentRecord::getMarksObtained)
                                .average().orElse(0.0))
                .average().orElse(0.0);
    }

    private ParentStudentProfileDTO.AssessmentTabDTO.GraphPointDTO point(
            String label, double avg, double classAvg) {
        return ParentStudentProfileDTO.AssessmentTabDTO.GraphPointDTO.builder()
                .label(label)
                .average(Math.round(avg * 10.0) / 10.0)
                .classAverage(Math.round(classAvg * 10.0) / 10.0)
                .build();
    }

    private String determineOverallRisk(List<AiAnalysis> analyses) {
        if (analyses.stream().anyMatch(a -> "HIGH".equals(a.getRiskLevel())))
            return "HIGH";
        if (analyses.stream().anyMatch(a -> "MEDIUM".equals(a.getRiskLevel())))
            return "MEDIUM";
        return analyses.isEmpty() ? "NOT_ANALYZED" : "LOW";
    }

    private String buildTrajectoryMessage(
            String name, String trajectory,
            double change, double prevMean,
            double currentMean, Term prevTerm) {
        if (prevMean <= 0) return name +
                "'s first term — no previous data to compare.";
        return switch (trajectory) {
            case "IMPROVING" -> String.format(
                    "%s improved from %.1f to %.1f (+%.1f marks) compared to %s. " +
                            "Great progress — keep it up!", name, prevMean,
                    currentMean, change,
                    prevTerm.name().replace("_", " "));
            case "DECLINING" -> String.format(
                    "%s's mean dropped from %.1f to %.1f (%.1f marks) compared to %s. " +
                            "Please speak to their teachers and increase study time.",
                    name, prevMean, currentMean, change,
                    prevTerm.name().replace("_", " "));
            default -> String.format(
                    "%s is maintaining a consistent mean of around %.1f marks. " +
                            "Encourage regular revision to improve further.",
                    name, currentMean);
        };
    }

    private String buildParentSummary(
            String name, String trajectory,
            String riskLevel, List<String> highRisk,
            List<String> mediumRisk, List<String> lowRisk) {
        StringBuilder sb = new StringBuilder();
        if (!highRisk.isEmpty())
            sb.append(String.format(
                    "%s urgently needs support in: %s. ",
                    name, String.join(", ", highRisk)));
        if (!mediumRisk.isEmpty())
            sb.append(String.format(
                    "Moderate attention needed in: %s. ",
                    String.join(", ", mediumRisk)));
        if (!lowRisk.isEmpty())
            sb.append(String.format(
                    "Performing well in: %s. ",
                    String.join(", ", lowRisk)));
        sb.append(switch (trajectory) {
            case "IMPROVING" -> "Overall trend is positive. Keep encouraging!";
            case "DECLINING" -> "Overall performance is declining. " +
                    "Consider scheduling a meeting with teachers.";
            default -> "Performance is stable. Regular revision is recommended.";
        });
        return sb.toString();
    }

    private List<String> parseActionPoints(String suggestion) {
        if (suggestion == null || suggestion.isBlank()) return List.of();
        return Arrays.stream(suggestion.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String buildOverallSuggestionMessage(
            String name, int needsAttention, int total) {
        if (needsAttention == 0)
            return name + " is performing well across all " + total +
                    " subjects. Keep encouraging consistent study habits.";
        if (needsAttention == total)
            return name + " needs urgent support across all subjects. " +
                    "Please review the recommendations below and speak to teachers.";
        return String.format(
                "%s needs focused attention in %d out of %d subjects. " +
                        "Review the recommendations below for each subject.",
                name, needsAttention, total);
    }

    private String ordinal(int n) {
        if (n >= 11 && n <= 13) return "th";
        return switch (n % 10) {
            case 1 -> "st"; case 2 -> "nd";
            case 3 -> "rd"; default -> "th";
        };
    }
}