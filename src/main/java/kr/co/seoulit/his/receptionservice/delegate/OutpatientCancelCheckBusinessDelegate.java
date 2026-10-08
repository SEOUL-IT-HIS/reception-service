package kr.co.seoulit.his.receptionservice.delegate;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import kr.co.seoulit.his.receptionservice.dto.response.OutpatientEncounterStatusResponsedto;

/**
 * 외래 접수 취소 직전, 외래 서비스에 그 접수의 진료 상태를 조회하는 RestTemplate 클라이언트.
 *
 * <p>외래에서 진료가 시작된 접수는 취소 메시지를 받아도 외래가 상태를 바꾸지 않으므로(IN_PROGRESS/COMPLETED 유지),
 * 접수에서 미리 막기 위한 사전 조회다. 서버 간 호출이라 로그인 없이 부르고, 외래 쪽 데이터는 바꾸지 않는다.
 * {@code outpatient.cancel-check.enabled=true} 일 때만 빈이 생성된다 (외래 API 배포·확인 전에는 꺼둔다).
 *
 * <p>짧은 타임아웃의 {@code emergencyActiveCheckRestTemplate} 을 같이 쓴다 (접수를 막지 않는 사전 조회 용도).
 */
@Component
@ConditionalOnProperty(name = "outpatient.cancel-check.enabled", havingValue = "true")
public class OutpatientCancelCheckBusinessDelegate {

    private final RestTemplate restTemplate;

    @Value("${outpatient.url}")
    private String outpatientUrl;

    public OutpatientCancelCheckBusinessDelegate(
            @Qualifier("emergencyActiveCheckRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 접수의 외래 진료 상태 조회.
     * - 404 는 "외래에 아직 등록되지 않은 접수"라는 외래의 약속이므로 빈 값을 돌려준다 (취소해도 되는 접수).
     * - 그 밖의 실패(타임아웃·5xx·본문 없음)는 예외로 전파한다 — 막을지는 서비스 레이어가 정한다.
     */
    public Optional<OutpatientEncounterStatusResponsedto> getStatus(String receptionId) {
        ParameterizedTypeReference<OutpatientApiResponse<OutpatientEncounterStatusResponsedto>> responseType =
                new ParameterizedTypeReference<>() {
                };
        try {
            OutpatientApiResponse<OutpatientEncounterStatusResponsedto> response = restTemplate.exchange(
                    outpatientUrl + "/api/outpatient/encounters/by-reception/{receptionId}/status",
                    HttpMethod.GET,
                    null,
                    responseType,
                    receptionId).getBody();
            if (response == null || response.data() == null) {
                throw new RestClientException("외래 진료 상태 응답에 data 가 없습니다. receptionId=" + receptionId);
            }
            return Optional.of(response.data());
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    /** 외래 서비스 응답 포맷 — code 가 문자열/숫자 어느 쪽이어도 받도록 Object 로 둔다. */
    private record OutpatientApiResponse<T>(Object code, String message, T data) {
    }
}
