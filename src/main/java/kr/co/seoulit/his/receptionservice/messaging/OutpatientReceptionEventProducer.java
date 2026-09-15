package kr.co.seoulit.his.receptionservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import kr.co.seoulit.his.receptionservice.messaging.event.ReceptionRegisteredEvent;
import lombok.RequiredArgsConstructor;

/**
 * 외래 서비스로 "외래 접수 등록" 이벤트를 Kafka로 전송하는 Producer.
 *
 * <p>{@code KafkaTemplate} 은 spring-kafka 자동설정으로 생성된 빈을 그대로 주입받는다.
 * 전송은 비동기이며, 실패해도 접수 등록 트랜잭션에는 영향을 주지 않는다(로그만 남긴다).
 */
@Component
@ConditionalOnProperty(name = "outpatient.kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutpatientReceptionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OutpatientReceptionEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${outpatient.kafka.topic}")
    private String topic;

    /**
     * 접수건당 1개의 이벤트를 발행한다. 파티션 키는 receptionId 로 잡아 동일 접수건의 순서를 보장한다.
     */
    public void send(ReceptionRegisteredEvent event) {
        String key = event.data().receptionId();

        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("외래 접수 이벤트 발행 실패 - topic={}, receptionId={}, eventId={}",
                        topic, key, event.eventId(), ex);
                return;
            }
            log.info("외래 접수 이벤트 발행 완료 - topic={}, receptionId={}, partition={}, offset={}",
                    topic, key,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        });
    }
}
