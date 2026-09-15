package kr.co.seoulit.his.receptionservice.messaging.event;

import java.time.LocalDateTime;

/**
 * 응급 접수 저장 직후 서비스가 발행하는 스프링 내부 이벤트(트랜잭션 경계 안).
 *
 * <p>실제 응급 서비스 REST 전송은 트랜잭션 커밋 이후
 * ({@code @TransactionalEventListener(AFTER_COMMIT)})에 이루어진다.
 * 커밋 시점에 엔티티/외부조회 상태에 의존하지 않도록, 전송에 필요한 값만 스냅샷으로 담는다.
 */
public record EmergencyReceptionRegisteredInternalEvent(
        String receptionId,
        String patientId,
        String arrivalPath,
        LocalDateTime receivedAt,
        String memo,
        String chiefComplaintRaw
) {
}
