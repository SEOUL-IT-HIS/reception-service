package kr.co.seoulit.his.receptionservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import kr.co.seoulit.his.receptionservice.messaging.event.ReceptionVoidedEvent;
import kr.co.seoulit.his.receptionservice.service.ReceptionService;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * 응급 서비스가 보내는 "오접수·중복 접수" 이벤트(ReceptionVoided)를 받아 해당 접수를 강제로 취소한다.
 *
 * <ul>
 *   <li>응급이 직접 결정한 취소이므로 응급 기록이 있어도 취소한다 (일반 취소의 응급 진료 기록 확인을 건너뜀).</li>
 *   <li>응급이 Kafka 장애에 대비해 같은 이벤트를 주기적으로 다시 보낸다 — 이미 취소된 접수는 그대로 두므로 안전하다.</li>
 *   <li>취소가 끝나면 일반 취소와 같은 ReceptionCancelled 이벤트가 응급 토픽으로 나간다.</li>
 * </ul>
 *
 * <p>값을 못 읽는 메시지(깨진 JSON 등)는 다시 읽어도 소용없으므로 로그만 남기고 넘어간다.
 * DB 장애 같은 일시 오류는 예외를 그대로 던져 Spring Kafka 의 재시도에 맡긴다.
 */
@Component
@ConditionalOnProperty(name = "emergency.voided.enabled", havingValue = "true")
@RequiredArgsConstructor
public class EmergencyReceptionVoidedListener {

    private static final Logger log = LoggerFactory.getLogger(EmergencyReceptionVoidedListener.class);

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final ReceptionService receptionService;

    @KafkaListener(topics = "${emergency.voided.topic}", groupId = "${emergency.voided.group-id}")
    public void handle(String payload) {
        ReceptionVoidedEvent event;
        try {
            event = MAPPER.readValue(payload, ReceptionVoidedEvent.class);
        } catch (Exception e) {
            log.error("[KAFKA_CONSUME_FAILED] 오접수 이벤트를 읽지 못해 건너뜀 - payload={}", abbreviate(payload), e);
            return;
        }
        if (!ReceptionVoidedEvent.EVENT_TYPE.equals(event.eventType())) {
            log.warn("오접수 토픽에서 모르는 eventType 을 건너뜀 - eventType={}, eventId={}", event.eventType(), event.eventId());
            return;
        }
        if (event.receptionId() == null || event.receptionId().isBlank()) {
            log.error("[KAFKA_CONSUME_FAILED] 오접수 이벤트에 receptionId 가 없어 건너뜀 - eventId={}", event.eventId());
            return;
        }
        log.info("오접수 이벤트 수신 - eventId={}, receptionId={}, reasonType={}", event.eventId(), event.receptionId(), event.reasonType());
        receptionService.voidEmergencyReception(
                event.receptionId(), event.reasonType(), event.reasonText(), event.keptReceptionId(), event.decidedBy());
    }

    private static String abbreviate(String s) {
        return s == null ? null : s.length() <= 300 ? s : s.substring(0, 300) + "...";
    }
}
