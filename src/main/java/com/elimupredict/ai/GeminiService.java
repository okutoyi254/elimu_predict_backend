package com.elimupredict.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public String generateSuggestion(String subjectName,
                                     double riskPercentage,
                                     List<Double> marks){
        try{
            String prompt = buildPrompt(subjectName,riskPercentage,marks);
            Map<String, Object> requestBody = Map.of(
                    "contents",List.of(
                            Map.of("parts",List.of(
                                    Map.of("text",prompt)
                            ))
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody,headers);

            String url = apiUrl + "?key="+apiKey;

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return parseGeminiResponse(response.getBody());
        }
        catch (Exception e){
            log.error("Gemini API call failed: {}",e.getMessage());
            return buildFallbackSuggestion(subjectName,riskPercentage);
        }
    }

    private String buildPrompt(String subjectName,
                               double riskPercentage,
                               List<Double> marks){
        return String.format(
                "A secondary school student has a %.1f%% risk of failing %s. " +
                "Their recent assessment scores are: %s. " +
                "Provide exactly 3 specific, practical study interventions " +
                "suitable for a secondary school student. " +
                "Be concise and use simple language. " +
                "Format as a numbered list.",
                riskPercentage, subjectName, marks.toString()

        );
    }

    @SuppressWarnings("unchecked")
    private String parseGeminiResponse(Map<String, Object> body){

        try{
            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) body.get("candidates");
            Map<String, Object> content =
                    (Map<String, Object>) candidates.getFirst().get("content");
            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) content.get("parts");
            return (String) parts.getFirst().get("text");
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            return null;
        }

    }

    private String buildFallbackSuggestion(String subjectName, double risk) {
        if (risk >= 70) {
            return String.format(
                    """
                            1. Attend extra tuition classes for %s immediately.
                            2. Review and redo all past CAT papers with a teacher.
                            3. Form a study group with top-performing classmates.""",
                    subjectName);
        } else if (risk >= 40) {
            return String.format(
                    """
                            1. Dedicate 30 minutes daily to %s revision.
                            2. Identify your weakest topics and focus on those first.
                            3. Complete all assignments and seek teacher feedback.""",
                    subjectName);
        }
        return "Keep up the good work! Maintain your study schedule and stay consistent.";
    }
}
