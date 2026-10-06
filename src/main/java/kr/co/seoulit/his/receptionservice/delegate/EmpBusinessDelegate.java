package kr.co.seoulit.his.receptionservice.delegate;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import kr.co.seoulit.his.receptionservice.common.ApiResponse;
import kr.co.seoulit.his.receptionservice.dto.response.EmpResponsedto;
import lombok.RequiredArgsConstructor;

/**
 * admin-service(직원 관리)와 통신하는 RestTemplate 클라이언트
 * - 담당의는 ADM 직원 중 의사 역할인 직원의 EMP_ID 로 저장되므로, 의사명은 직원 목록에서 찾는다.
 * - 로그인 세션이 필요하다 (OutboundSessionForwardingInterceptor 가 호출자 쿠키를 실어 보낸다).
 */
@Component
@RequiredArgsConstructor
public class EmpBusinessDelegate {

    private final RestTemplate restTemplate;

    @Value("${admin.url}")
    private String adminUrl;

    /** 직원 전체 목록 조회 */
    public ApiResponse<List<EmpResponsedto>> getEmps() {
        ParameterizedTypeReference<ApiResponse<List<EmpResponsedto>>> responseType =
                new ParameterizedTypeReference<>() {
                };
        return restTemplate.exchange(
                adminUrl + "/api/admin/emp/list",
                HttpMethod.GET,
                null,
                responseType).getBody();
    }

}
