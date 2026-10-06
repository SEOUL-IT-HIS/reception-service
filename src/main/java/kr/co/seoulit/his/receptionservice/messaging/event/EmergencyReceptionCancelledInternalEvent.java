package kr.co.seoulit.his.receptionservice.messaging.event;

import java.time.LocalDateTime;

/**
 * 응급 접수 취소 직후 서비스가 발행하는 스프링 내부 이벤트(트랜잭션 경계 안).
 *
 * <p>취소 이벤트도 등록 때와 같은 값으로 모든 필드를 채워 보내기 위해, 등록 스냅샷과 같은 모양
 * ({@link EmergencyReceptionRegisteredInternalEvent})을 그대로 담는다.
 * 등록용 내부 이벤트 타입을 직접 쓰지 않는 이유: 그 타입은 REST/Kafka "등록" 리스너가 구독하므로
 * 취소인데 등록 전송이 한 번 더 나가버린다.
 */
public record EmergencyReceptionCancelledInternalEvent(
        EmergencyReceptionRegisteredInternalEvent snapshot,
        LocalDateTime cancelledAt
) {
}
