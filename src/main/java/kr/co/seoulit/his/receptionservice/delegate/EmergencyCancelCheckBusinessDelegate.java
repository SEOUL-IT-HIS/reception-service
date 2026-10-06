package kr.co.seoulit.his.receptionservice.delegate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import kr.co.seoulit.his.receptionservice.dto.response.EmergencyCancellableResponsedto;

/**
 * 응급 접수 취소 직전, 응급 서비스에 그 접수를 취소해도 되는지(진료 기록이 없는지) 조회하는 RestTemplate 클라이언트.
 *
 * <p>응급은 진료 기록이 있는 접수의 취소 이벤트를 받아도 취소하지 않으므로, 접수에서 미리 막기 위한 사전 조회다.
 * 서버 간 호출이라 로그인 없이 부를 수 있고, 응급 쪽 데이터는 바꾸지 않는다.
 * {@code emergency.cancel-check.enabled=false} 면 이 빈이 생성되지 않는다 (응급 배포 전).
 *
 * <p>짧은 타임아웃의 {@code emergencyActiveCheckRestTemplate} 을 같이 쓴다 (응급 서비스 사전 조회 용도로 같다).
 */
@Component
@ConditionalOnProperty(name = "emergency.cancel-check.enabled", havingValue = "true")
public class EmergencyCancelCheckBusinessDelegate {

    private final RestTemplate restTemplate;

    @Value("${emergency.url}")
    private String emergencyUrl;

    public EmergencyCancelCheckBusinessDelegate(
            @Qualifier("emergencyActiveCheckRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 취소 가능 여부 조회. 호출 실패/타임아웃은 그대로 전파한다 — 취소를 막을지(fail-closed)는 서비스 레이어가 판단한다.
     */
    public EmergencyCancellableResponsedto getCancellable(String receptionId) {
        ParameterizedTypeReference<EmergencyApiResponse<EmergencyCancellableResponsedto>> responseType =
                new ParameterizedTypeReference<>() {
                };
        EmergencyApiResponse<EmergencyCancellableResponsedto> response = restTemplate.exchange(
                emergencyUrl + "/api/emergency/care/reception-intakes/cancellable?receptionId={receptionId}",
                HttpMethod.GET,
                null,
                responseType,
                receptionId).getBody();
        return response != null ? response.data() : null;
    }

    /** 응급 서비스 응답 포맷: {@code code} 가 문자열인 것만 제외하면 reception-service의 ApiResponse와 동일 */
    private record EmergencyApiResponse<T>(String code, String message, T data) {
    }
}
