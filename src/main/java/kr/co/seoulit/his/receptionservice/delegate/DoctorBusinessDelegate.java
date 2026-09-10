package kr.co.seoulit.his.receptionservice.delegate;

import kr.co.seoulit.his.receptionservice.common.ApiResponse;
import kr.co.seoulit.his.receptionservice.dto.response.DoctorResponsedto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * admin-service(의사 관리)와 통신하는 RestTemplate 클라이언트
 */
@Component
@RequiredArgsConstructor
public class DoctorBusinessDelegate {

    private final RestTemplate restTemplate;

    @Value("${admin.url}")
    private String adminUrl;

    /**
     * 진료과별 의사 목록 조회
     */
    public ApiResponse<List<DoctorResponsedto>> getDoctorsByDepartment(String deptId) {
        ParameterizedTypeReference<ApiResponse<List<DoctorResponsedto>>> responseType = new ParameterizedTypeReference<>() {
        };
        return restTemplate.exchange(
                adminUrl + "/api/departments/{deptId}/doctors",
                HttpMethod.GET,
                null,
                responseType,
                deptId).getBody();
    }

}
