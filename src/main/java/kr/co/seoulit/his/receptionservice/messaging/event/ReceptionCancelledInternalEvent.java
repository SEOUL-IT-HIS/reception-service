package kr.co.seoulit.his.receptionservice.messaging.event;

import java.time.LocalDateTime;

import kr.co.seoulit.his.receptionservice.entity.ReceptionEntity;

/**
 * 외래 접수 취소 직후 서비스가 발행하는 스프링 내부 이벤트(트랜잭션 경계 안).
 *
 * <p>외래(OPD)는 취소 이벤트도 등록 때와 같은 필드가 모두 채워져 있어야 하므로(null 이면 기존 값이 지워짐),
 * 등록 스냅샷({@link ReceptionRegisteredInternalEvent})을 그대로 담는다. status 는 이미 CANCELLED 로 바뀐 상태다.
 * 실제 Kafka 발행은 트랜잭션 커밋 이후에 이루어진다.
 */
public record ReceptionCancelledInternalEvent(
        ReceptionRegisteredInternalEvent snapshot,
        LocalDateTime cancelledAt
) {

    public static ReceptionCancelledInternalEvent from(ReceptionEntity reception, LocalDateTime cancelledAt) {
        return new ReceptionCancelledInternalEvent(ReceptionRegisteredInternalEvent.from(reception), cancelledAt);
    }
}
