package kr.co.seoulit.his.receptionservice.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 응급 서비스로 전달하는 "응급 접수 취소" Kafka 이벤트. 등록 이벤트({@link ReceptionIntakeEvent})와
 * 같은 토픽·같은 receptionId 로 발행한다.
 *
 * <p>필드는 등록 이벤트와 같은 이름·같은 값으로 모두 채우고, 끝에 eventType("ReceptionCancelled")과
 * status("CANCELLED")를 붙인다. 응급 서비스가 이 두 필드로 등록/취소를 구분해야 하므로,
 * 응급 쪽 반영 전에는 발행하지 않는다 ({@code emergency.kafka.cancel-enabled}).
 */
public record ReceptionIntakeCancelledEvent(
        String eventId,
        LocalDateTime occurredAt,
        String receptionId,
        String patientId,
        String arrivalPath,
        LocalDateTime receivedAt,
        String memo,
        String chiefComplaintRaw,
        Integer ktasLevel,
        LocalDateTime triageDateTime,
        String eventType,
        String status
) {

    public static final String EVENT_TYPE_CANCELLED = "ReceptionCancelled";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static ReceptionIntakeCancelledEvent from(EmergencyReceptionCancelledInternalEvent cancelled) {
        EmergencyReceptionRegisteredInternalEvent snapshot = cancelled.snapshot();
        return new ReceptionIntakeCancelledEvent(
                UUID.randomUUID().toString(),
                cancelled.cancelledAt(),
                snapshot.receptionId(),
                snapshot.patientId(),
                snapshot.arrivalPath(),
                snapshot.receivedAt(),
                snapshot.memo(),
                snapshot.chiefComplaintRaw(),
                snapshot.ktasLevel(),
                snapshot.triageDateTime(),
                EVENT_TYPE_CANCELLED,
                STATUS_CANCELLED);
    }
}
