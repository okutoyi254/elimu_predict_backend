package com.elimupredict.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.elimupredict.ai.dto.MlRequest;
import com.elimupredict.ai.dto.MlResponse;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MlService {

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    private static final boolean USE_REAL_ML_SERVICE = false;

    private final RestTemplate restTemplate;

    public MlResponse predict(MlRequest request) {
        if (USE_REAL_ML_SERVICE) {
            return callRealMlService(request);
        }
        return calculateStubRisk(request);
    }

    private MlResponse calculateStubRisk(MlRequest request){

        List<Double> marks = request.getMarks();

//        Weighted average: CATs = 30%, Exams = 70%
        double avg;

        if(marks.size() == 5){
            double catAvg = (marks.get(0)+ marks.get(1)+ marks.get(2)) / 3.0;
            double examAvg = (marks.get(3)+ marks.get(4)) / 2.0;
            avg = (catAvg * 0.30) + (examAvg * 0.70);
        }
        else {
            avg = marks.stream().mapToDouble(Double::doubleValue).average().orElse(50.0);
        }

//        Trend penalty - if marks are declining, increase risk
        double trend = 0;
        if(marks.size() >=2){
            trend = marks.get(marks.size()-1)- marks.getFirst();
        }

//        Risk = inverse of average, adjusted for trend
        double rawRisk = 100.0 - avg;
        if(trend < -10) rawRisk +=10;
        if(trend >10) rawRisk -=5;

        double riskPercentage = Math.min(100.0,Math.max(0.0, rawRisk));

        String riskLevel = riskPercentage >= 70 ? "HIGH"
                : riskPercentage >=40  ? "MEDIUM"
                : "LOW";

//        Weakness group: 0= low risk, 1= medium, 2=high
        int group = riskPercentage >=70 ? 2
                : riskPercentage >=40 ? 1 : 0;

        log.info("[STUB ML] Student: {} | Avg: {} | Risk: {}% | Level: {}",
                request.getAdmissionNumber(), String.format("%.1f", avg), String.format("%.1f", riskPercentage), riskLevel);

        MlResponse response = new MlResponse();
        response.setAdmissionNumber(request.getAdmissionNumber());
        response.setSubjectId(request.getSubjectId());
        response.setRiskPercentage(Math.round(riskPercentage * 10.0) / 10.0);
        response.setRiskLevel(riskLevel);
        response.setWeaknessGroup(group);
        return response;
    }

//FlaskApi call request
    private MlResponse callRealMlService(MlRequest request){

        try{
            String url = mlServiceUrl+"/predict";
            return  restTemplate.postForObject(url,request, MlResponse.class);
        }
        catch(Exception ex){
            log.error("ML service unavailable: {}, Falling back to stub.",ex.getMessage());
            return calculateStubRisk(request);
        }
    }
}
