package kr.co.seoulit.his.receptionservice.messaging.event;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 외래 서비스로 전달하는 "외래 접수 등록/취소" 이벤트. (eventType 으로 구분)
 *
 * <p>외래 서비스가 제공한
 * {@code kr.co.seoulit.his.outpatientservice.common.client.reception.ReceptionEventDto}
 * 의 {@code ReceptionRegisteredEvent} / {@code ReceptionData} 구조와 JSON 필드가
 * 1:1로 일치하도록 복제한 계약(contract) 클래스다. (서로 다른 서비스라 원본을 직접 import 할 수 없다.)
 * 필드명·순서를 임의로 바꾸면 외래 측 역직렬화가 깨지므로 변경 시 반드시 외래와 합의한다.
 */
public record ReceptionRegisteredEvent(
        String eventId,
        String eventType,
        String version,
        OffsetDateTime occurredAt,
        String source,
        ReceptionData data
) {

    public static final String EVENT_TYPE_REGISTERED = "ReceptionRegistered";
    public static final String EVENT_TYPE_CANCELLED = "ReceptionCancelled";
    /** 1.1 — data.visitType, data.receptionType 추가 (OPD 합의, 필드 추가만 있는 하위호환 변경) */
    public static final String SCHEMA_VERSION = "1.1";
    public static final String SOURCE_RCP = "RCP";

    public record ReceptionData(
            String receptionId,      // RCP 접수 ID
            String patientId,        // PAT 환자 ID
            String departmentCode,   // 배정된 진료과 (RCP deptId 를 문자열로 변환)
            String doctorId,         // 담당의 ID
            LocalDate visitDate,     // 내원일 (= 접수일)
            String status,           // 접수 상태 (등록: "RECEPTION", 취소: "CANCELLED")
            String visitReason,      // 방문 사유
            String visitType,        // 초진/재진 — INITIAL / REVISIT (1.1~)
            String receptionType     // 예약/당일 — RESERVATION / WALK_IN (1.1~)
    ) {}

    /**
     * 커밋 후 스냅샷({@link ReceptionRegisteredInternalEvent})을 외부 전달용 이벤트로 변환한다.
     */
    public static ReceptionRegisteredEvent from(ReceptionRegisteredInternalEvent snapshot) {
        return of(EVENT_TYPE_REGISTERED,
                snapshot.receptionDate().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                snapshot);
    }

    /**
     * 접수 취소 이벤트. 같은 토픽·같은 receptionId 로 발행하고, data 는 등록 때와 같은 값으로 채운다
     * (OPD 는 receptionId 로 기존 건을 찾아 갱신하며, null 필드는 기존 값을 지운다).
     * data.status 는 "CANCELLED", occurredAt 은 취소 시각이다.
     */
    public static ReceptionRegisteredEvent cancelled(ReceptionRegisteredInternalEvent snapshot, OffsetDateTime cancelledAt) {
        return of(EVENT_TYPE_CANCELLED, cancelledAt, snapshot);
    }

    private static ReceptionRegisteredEvent of(
            String eventType, OffsetDateTime occurredAt, ReceptionRegisteredInternalEvent snapshot) {
        return new ReceptionRegisteredEvent(
                UUID.randomUUID().toString(),
                eventType,
                SCHEMA_VERSION,
                occurredAt,
                SOURCE_RCP,
                new ReceptionData(
                        snapshot.receptionId(),
                        snapshot.patientId(),
                        snapshot.deptId(),
                        snapshot.doctorId(),
                        snapshot.receptionDate().toLocalDate(),
                        snapshot.status(),
                        snapshot.visitReason(),
                        snapshot.visitType(),
                        snapshot.receptionType()
                )
        );
    }
}
