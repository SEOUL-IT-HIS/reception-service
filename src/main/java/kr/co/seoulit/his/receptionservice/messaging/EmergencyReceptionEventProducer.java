package kr.co.seoulit.his.receptionservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import kr.co.seoulit.his.receptionservice.messaging.event.ReceptionIntakeCancelledEvent;
import kr.co.seoulit.his.receptionservice.messaging.event.ReceptionIntakeEvent;
import lombok.RequiredArgsConstructor;

/**
 * 응급 서비스로 "응급 접수 접수내역" 이벤트를 Kafka로 전송하는 Producer.
 *
 * <p>{@code KafkaTemplate} 은 spring-kafka 자동설정으로 생성된 빈을 그대로 주입받는다.
 * 전송은 비동기이며, 실패해도 응급접수 등록 트랜잭션에는 영향을 주지 않는다(로그만 남긴다).
 */
@Component
@ConditionalOnProperty(name = "emergency.kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class EmergencyReceptionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(EmergencyReceptionEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${emergency.kafka.topic}")
    private String topic;

    /**
     * 접수건당 1개의 이벤트를 발행한다. 파티션 키는 receptionId 로 잡아 동일 접수건의 순서를 보장한다.
     */
    public void send(ReceptionIntakeEvent event) {
        send("응급 접수 이벤트", event.receptionId(), event.eventId(), event);
    }

    /** 응급 접수 취소 이벤트 — 등록과 같은 토픽·같은 키(receptionId)로 발행해 순서를 보장한다. */
    public void send(ReceptionIntakeCancelledEvent event) {
        send("응급 접수 취소 이벤트", event.receptionId(), event.eventId(), event);
    }

    private void send(String label, String key, String eventId, Object event) {
        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[KAFKA_PUBLISH_FAILED] {} 발행 실패 - topic={}, receptionId={}, eventId={}",
                        label, topic, key, eventId, ex);
                return;
            }
            log.info("{} 발행 완료 - topic={}, receptionId={}, partition={}, offset={}",
                    label, topic, key,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        });
    }
}
