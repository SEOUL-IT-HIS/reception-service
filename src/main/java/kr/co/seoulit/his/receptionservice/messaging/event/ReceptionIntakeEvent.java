package kr.co.seoulit.his.receptionservice.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 응급 서비스로 전달하는 "응급 접수 접수내역" Kafka 이벤트.
 *
 * <p>응급 서비스가 제공한 {@code ReceptionIntakeEvent}(receptionId, patientId, arrivalPath,
 * receivedAt, memo, chiefComplaintRaw + eventId, occurredAt 메타필드) 와 JSON 필드가 1:1로
 * 일치하도록 복제한 계약(contract) 클래스다. (서로 다른 서비스라 원본을 직접 import 할 수 없다.)
 * 필드명·순서를 임의로 바꾸면 응급 측 역직렬화가 깨지므로 변경 시 반드시 응급과 합의한다.
 *
 * <p>REST 전송({@link kr.co.seoulit.his.receptionservice.dto.request.EmergencyIntakeRequestdto})과
 * 데이터 필드는 동일하고, eventId(멱등성 체크/추적용)·occurredAt(RCP에서 이벤트 발생 시간)만
 * Kafka 이벤트 쪽에 메타필드로 추가된다.
 */
public record ReceptionIntakeEvent(
        String eventId,
        LocalDateTime occurredAt,
        String receptionId,
        String patientId,
        String arrivalPath,
        LocalDateTime receivedAt,
        String memo,
        String chiefComplaintRaw
) {

    public static ReceptionIntakeEvent from(EmergencyReceptionRegisteredInternalEvent snapshot) {
        return new ReceptionIntakeEvent(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                snapshot.receptionId(),
                snapshot.patientId(),
                snapshot.arrivalPath(),
                snapshot.receivedAt(),
                snapshot.memo(),
                snapshot.chiefComplaintRaw());
    }
}
