package kr.co.seoulit.his.receptionservice.delegate;

import kr.co.seoulit.his.receptionservice.common.ApiResponse;
import kr.co.seoulit.his.receptionservice.dto.response.PatientDetailResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.PatientSummaryResponsedto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * CB2(환자관리 서비스)와 통신하는 RestTemplate 클라이언트
 */
@Component
@RequiredArgsConstructor
public class PatientBusinessDelegate {

    private final RestTemplate restTemplate;

    @Value("${cb2.url}")
    private String cb2Url;

    /**
     * 환자 목록 조회
     * CB2는 배열을 바로 내려주지 않고 {code,message,data} 로 감싸서 응답한다.
     */
    public ApiResponse<List<PatientSummaryResponsedto>> getPatients() {
        ParameterizedTypeReference<ApiResponse<List<PatientSummaryResponsedto>>> responseType = new ParameterizedTypeReference<>() {
        };
        return restTemplate.exchange(
                cb2Url + "/api/patient/list",
                HttpMethod.GET,
                null,
                responseType).getBody();
    }

    /**
     * 환자 상세 조회
     */
    public PatientDetailResponsedto getPatientById(String patientId) {
        return restTemplate.getForObject(
                cb2Url + "/patients/{patientId}",
                PatientDetailResponsedto.class,
                patientId);
    }

}
