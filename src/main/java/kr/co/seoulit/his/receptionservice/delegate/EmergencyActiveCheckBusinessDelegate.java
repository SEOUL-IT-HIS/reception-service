package kr.co.seoulit.his.receptionservice.delegate;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import kr.co.seoulit.his.receptionservice.dto.response.ActiveEmergencyReceptionResponsedto;
import lombok.RequiredArgsConstructor;

/**
 * 응급 서비스(emergency-service)에 같은 환자의 진행 중(미퇴실) 응급접수가 있는지 조회하는 RestTemplate 클라이언트.
 *
 * <p>응급접수 등록 "직전"에 경고 여부만 판단하기 위한 사전 조회이며, 등록 트랜잭션과는 무관하다.
 * {@code emergency.active-check.enabled=false} 면 이 빈이 생성되지 않는다.
 *
 * <p>응급 서비스의 공통 응답 포맷은 {@code code} 가 문자열("SUCCESS")이라 reception-service 자체의
 * {@code ApiResponse}(code: int)와 모양이 달라서 그대로 재사용할 수 없다. 그래서 이 응답만 별도로 받는다.
 */
@Component
@ConditionalOnProperty(name = "emergency.active-check.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class EmergencyActiveCheckBusinessDelegate {

    @Qualifier("emergencyActiveCheckRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${emergency.url}")
    private String emergencyUrl;

    /**
     * 진행 중(미퇴실) 응급접수 목록 조회. 퇴실 처리된 건은 응급 서비스가 이미 제외해서 내려준다.
     * 호출 실패/타임아웃은 여기서 처리하지 않고 그대로 전파한다 — fail-open 판단은 호출측(서비스 레이어)의 책임이다.
     */
    public List<ActiveEmergencyReceptionResponsedto> getActiveReceptions(String patientId) {
        ParameterizedTypeReference<EmergencyApiResponse<List<ActiveEmergencyReceptionResponsedto>>> responseType =
                new ParameterizedTypeReference<>() {
                };
        EmergencyApiResponse<List<ActiveEmergencyReceptionResponsedto>> response = restTemplate.exchange(
                emergencyUrl + "/api/emergency/care/patients/active?patientId={patientId}",
                HttpMethod.GET,
                null,
                responseType,
                patientId).getBody();
        return response != null && response.data() != null ? response.data() : List.of();
    }

    /** 응급 서비스 응답 포맷: {@code code} 가 문자열인 것만 제외하면 reception-service의 ApiResponse와 동일 */
    private record EmergencyApiResponse<T>(String code, String message, T data) {
    }
}
