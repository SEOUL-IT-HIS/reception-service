package kr.co.seoulit.his.receptionservice.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 응급 서비스가 "오접수·중복 접수"로 접수를 닫았을 때 보내는 이벤트 (토픽 emergency.reception.voided.v1, 키 receptionId).
 *
 * <p>응급과 합의한 평평한 규격과 JSON 필드가 1:1로 일치한다. 모르는 필드가 추가돼도 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReceptionVoidedEvent(
        String eventId,
        String eventType,       // "ReceptionVoided"
        String version,         // "1.0"
        String source,          // "ER"
        String occurredAt,      // ISO-8601 (오프셋 포함)
        String receptionId,
        String patientId,
        String reasonType,      // DUPLICATE / ERRONEOUS
        String reasonText,      // 500자 이내
        String keptReceptionId, // 중복일 때 남기는 접수 ID, 아니면 null
        String decidedBy        // 직원 ID (36자)
) {

    public static final String EVENT_TYPE = "ReceptionVoided";
}
