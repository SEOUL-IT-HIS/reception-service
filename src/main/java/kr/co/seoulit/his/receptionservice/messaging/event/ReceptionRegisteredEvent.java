package kr.co.seoulit.his.receptionservice.messaging.event;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 외래 서비스로 전달하는 "외래 접수 등록" 이벤트.
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
    public static final String SCHEMA_VERSION = "1.0";
    public static final String SOURCE_RCP = "RCP";

    public record ReceptionData(
            String receptionId,      // RCP 접수 ID
            String patientId,        // PAT 환자 ID
            String departmentCode,   // 배정된 진료과 (RCP deptId 를 문자열로 변환)
            String doctorId,         // 담당의 ID
            LocalDate visitDate,     // 내원일 (= 접수일)
            String status,           // 접수 상태 (RCP 초기값 "RECEPTION" 을 그대로 전달)
            String visitReason       // 방문 사유
    ) {}

    /**
     * 커밋 후 스냅샷({@link ReceptionRegisteredInternalEvent})을 외부 전달용 이벤트로 변환한다.
     */
    public static ReceptionRegisteredEvent from(ReceptionRegisteredInternalEvent snapshot) {
        return new ReceptionRegisteredEvent(
                UUID.randomUUID().toString(),
                EVENT_TYPE_REGISTERED,
                SCHEMA_VERSION,
                snapshot.receptionDate().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                SOURCE_RCP,
                new ReceptionData(
                        snapshot.receptionId(),
                        snapshot.patientId(),
                        snapshot.deptId(),
                        snapshot.doctorId(),
                        snapshot.receptionDate().toLocalDate(),
                        snapshot.status(),
                        snapshot.visitReason()
                )
        );
    }
}
