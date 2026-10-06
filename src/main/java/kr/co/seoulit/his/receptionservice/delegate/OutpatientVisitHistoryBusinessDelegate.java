package kr.co.seoulit.his.receptionservice.delegate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import kr.co.seoulit.his.receptionservice.dto.response.OutpatientVisitHistoryResponsedto;

/**
 * 외래 서비스(outpatient-service)에 환자의 외래 진료 이력이 있는지 조회하는 RestTemplate 클라이언트.
 *
 * <p>접수 화면의 초진/재진 자동 판정용 사전 조회다. 서버 간 호출이라 로그인 없이 부르고, 외래 쪽 데이터는 바꾸지 않는다.
 * 짧은 타임아웃의 {@code emergencyActiveCheckRestTemplate} 을 같이 쓴다 (접수를 막지 않는 사전 조회 용도).
 * {@code outpatient.visit-history.enabled=true} 일 때만 빈이 생성된다 (외래 API 배포 전에는 꺼둔다).
 *
 * <p>진료과·기간 조건은 쓰지 않는다 — 진료과 무관, 기간 제한 없이 "외래 진료 기록이 한 번이라도 있으면 재진".
 */
@Component
@ConditionalOnProperty(name = "outpatient.visit-history.enabled", havingValue = "true")
public class OutpatientVisitHistoryBusinessDelegate {

    private final RestTemplate restTemplate;

    @Value("${outpatient.url}")
    private String outpatientUrl;

    public OutpatientVisitHistoryBusinessDelegate(
            @Qualifier("emergencyActiveCheckRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 외래 진료 이력 조회. 호출 실패/타임아웃은 그대로 전파한다 — 판정하지 않고 직접 선택하게 할지는 서비스 레이어가 정한다.
     */
    public OutpatientVisitHistoryResponsedto getVisitHistory(String patientId) {
        ParameterizedTypeReference<OutpatientApiResponse<OutpatientVisitHistoryResponsedto>> responseType =
                new ParameterizedTypeReference<>() {
                };
        OutpatientApiResponse<OutpatientVisitHistoryResponsedto> response = restTemplate.exchange(
                outpatientUrl + "/api/outpatient/encounters/visit-history?patientId={patientId}",
                HttpMethod.GET,
                null,
                responseType,
                patientId).getBody();
        return response != null ? response.data() : null;
    }

    /** 외래 서비스 응답 포맷 — code 가 문자열/숫자 어느 쪽이어도 받도록 Object 로 둔다. */
    private record OutpatientApiResponse<T>(Object code, String message, T data) {
    }
}
