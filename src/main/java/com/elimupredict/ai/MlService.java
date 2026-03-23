package com.elimupredict.ai;

import com.elimupredict.ai.dto.MlRequest;
import com.elimupredict.ai.dto.MlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MlService {

    @Value("${ml.service.url}")
    private String mlServiceUrl;


    private static final boolean USE_REAL_ML_SERVICE = true;

    private final RestTemplate restTemplate;

    public MlResponse predict(MlRequest request) {
        if (USE_REAL_ML_SERVICE) {
            return callRealMlService(request);
        }
        return calculateStubRisk(request);
    }


    private MlResponse callRealMlService(MlRequest request) {
        try {
            String url = mlServiceUrl + "/predict";

            // Match Flask API field names
            java.util.Map<String, Object> payload = java.util.Map.of(
                    "admissionNumber", request.getAdmissionNumber(),
                    "subjectId", request.getSubjectId()
            );

            org.springframework.http.HttpHeaders headers =
                    new org.springframework.http.HttpHeaders();
            headers.setContentType(
                    org.springframework.http.MediaType.APPLICATION_JSON);

            org.springframework.http.HttpEntity<java.util.Map<String, Object>>
                    entity = new org.springframework.http.HttpEntity<>(
                    payload, headers);

            org.springframework.http.ResponseEntity<java.util.Map> response =
                    restTemplate.postForEntity(url, entity, java.util.Map.class);

            java.util.Map body = response.getBody();

            if (body == null || body.containsKey("error")) {
                String error = body != null ?
                        body.get("error").toString() : "null response";
                log.warn("[ML SERVICE] Returned error for student {}: {} " +
                                "— falling back to stub",
                        request.getAdmissionNumber(), error);
                return calculateStubRisk(request);
            }

            // Parse response
            double riskPercentage = parseDouble(body.get("riskPercentage"));
            String riskLevel = body.get("riskLevel") != null ?
                    body.get("riskLevel").toString() :
                    deriveRiskLevel(riskPercentage);
            int weaknessGroup = body.get("weaknessGroup") != null ?
                    ((Number) body.get("weaknessGroup")).intValue() :
                    deriveWeaknessGroup(riskLevel);

            log.info("[ML SERVICE] ✓ Student: {} | Risk: {}% | Level: {}",
                    request.getAdmissionNumber(), riskPercentage, riskLevel);

            MlResponse mlResponse = new MlResponse();
            mlResponse.setAdmissionNumber(request.getAdmissionNumber());
            mlResponse.setSubjectId(request.getSubjectId());
            mlResponse.setRiskPercentage(riskPercentage);
            mlResponse.setRiskLevel(riskLevel);
            mlResponse.setWeaknessGroup(weaknessGroup);
            mlResponse.setConfidence(body.get("confidence") != null ?
                    parseDouble(body.get("confidence")) : 0.87);
            return mlResponse;

        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Network error — service is down or unreachable
            log.error("[ML SERVICE] Unreachable at {} — {}. " +
                            "Using stub fallback for student {}.",
                    mlServiceUrl, e.getMessage(),
                    request.getAdmissionNumber());
            return calculateStubRisk(request);

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("[ML SERVICE] Client error {} for student {} — {}. " +
                            "Using stub fallback.",
                    e.getStatusCode(),
                    request.getAdmissionNumber(),
                    e.getResponseBodyAsString());
            return calculateStubRisk(request);

        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("[ML SERVICE] Server error 500 for student {} — {}. " +
                            "Using stub fallback.",
                    request.getAdmissionNumber(),
                    e.getResponseBodyAsString());
            return calculateStubRisk(request);

        } catch (Exception e) {
            log.error("[ML SERVICE] Unexpected error for student {} — {}. " +
                            "Using stub fallback.",
                    request.getAdmissionNumber(), e.getMessage());
            return calculateStubRisk(request);
        }
    }


    private MlResponse calculateStubRisk(MlRequest request) {
        List<Double> marks = request.getMarks();

        log.info("[STUB FALLBACK] Computing risk for student {} " +
                        "using weighted average model",
                request.getAdmissionNumber());

        // Weighted average: CATs = 30%, Exams = 70%
        double avg;
        if (marks != null && marks.size() == 5) {
            double catAvg = (marks.get(0) + marks.get(1) + marks.get(2)) / 3.0;
            double examAvg = (marks.get(3) + marks.get(4)) / 2.0;
            avg = (catAvg * 0.30) + (examAvg * 0.70);
        } else {
            avg = marks != null ?
                    marks.stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(50.0)
                    : 50.0;
        }

        // Trend via linear regression
        double trend = calculateTrend(marks);

        // Risk = inverse performance + trend adjustment
        double riskPercentage = 100.0 - avg;
        if (trend < -10) riskPercentage += 15;
        if (trend < -5)  riskPercentage += 7;
        if (trend > 10)  riskPercentage -= 10;
        if (trend > 5)   riskPercentage -= 5;

        riskPercentage = Math.min(100.0, Math.max(0.0, riskPercentage));
        riskPercentage = Math.round(riskPercentage * 10.0) / 10.0;

        String riskLevel = deriveRiskLevel(riskPercentage);
        int group = deriveWeaknessGroup(riskLevel);

        MlResponse response = new MlResponse();
        response.setAdmissionNumber(request.getAdmissionNumber());
        response.setSubjectId(request.getSubjectId());
        response.setRiskPercentage(riskPercentage);
        response.setRiskLevel(riskLevel);
        response.setWeaknessGroup(group);
        response.setConfidence(0.85);
        return response;
    }


    private double calculateTrend(List<Double> marks) {
        if (marks == null || marks.size() < 2) return 0.0;
        int n = marks.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (marks.get(i) == null) continue;
            sumX += i; sumY += marks.get(i);
            sumXY += (double) i * marks.get(i);
            sumX2 += (double) i * i;
            count++;
        }
        if (count < 2) return 0.0;
        double denom = (count * sumX2 - sumX * sumX);
        if (denom == 0) return 0.0;
        return ((count * sumXY - sumX * sumY) / denom) * 5;
    }


    private double parseDouble(Object value) {
        if (value == null) return 50.0;
        try { return ((Number) value).doubleValue(); }
        catch (Exception e) {
            try { return Double.parseDouble(value.toString()); }
            catch (Exception ex) { return 50.0; }
        }
    }

    private String deriveRiskLevel(double riskPercentage) {
        return riskPercentage >= 70 ? "HIGH"
                : riskPercentage >= 40 ? "MEDIUM"
                : "LOW";
    }

    private int deriveWeaknessGroup(String riskLevel) {
        return "HIGH".equals(riskLevel) ? 2
                : "MEDIUM".equals(riskLevel) ? 1
                : 0;
    }
}
