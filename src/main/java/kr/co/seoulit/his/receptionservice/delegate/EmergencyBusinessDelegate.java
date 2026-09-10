package kr.co.seoulit.his.receptionservice.delegate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import kr.co.seoulit.his.receptionservice.dto.request.EmergencyIntakeRequestdto;
import lombok.RequiredArgsConstructor;

/**
 * 응급 서비스(emergency-service)와 통신하는 RestTemplate 클라이언트.
 *
 * <p>ER 접수 등록 건을 응급 서비스로 단건 전달한다.
 * {@code emergency.intake.enabled=false} 면 이 빈이 생성되지 않는다.
 */
@Component
@ConditionalOnProperty(name = "emergency.intake.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class EmergencyBusinessDelegate {

    private final RestTemplate restTemplate;

    @Value("${emergency.url}")
    private String emergencyUrl;

    /**
     * 응급 접수 접수내역 단건 전달.
     * 응답 본문은 사용하지 않는다(2xx 이 아니면 예외가 발생하고, 호출측에서 로그만 남긴다).
     */
    public void sendReceptionIntake(EmergencyIntakeRequestdto request) {
        restTemplate.postForEntity(
                emergencyUrl + "/api/emergency/care/reception-intakes",
                request,
                Void.class);
    }
}
