package kr.co.seoulit.his.receptionservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import kr.co.seoulit.his.receptionservice.messaging.event.EmergencyReceptionRegisteredInternalEvent;
import kr.co.seoulit.his.receptionservice.messaging.event.ReceptionIntakeEvent;
import lombok.RequiredArgsConstructor;

/**
 * ER 접수 저장 트랜잭션이 <b>정상 커밋된 후에만</b> 응급 서비스로 접수내역을 Kafka로 발행한다.
 *
 * <p>{@link EmergencyReceptionIntakeListener}(REST 전송)와 같은 내부 이벤트
 * ({@link EmergencyReceptionRegisteredInternalEvent})를 각자 독립적으로 구독하는 별개 경로다.
 * REST 쪽 코드는 건드리지 않고, Kafka 발행만 추가한 것 — 둘 중 하나가 실패해도 다른 하나에는
 * 영향이 없다.
 *
 * <ul>
 *   <li>트랜잭션이 롤백되면 이벤트도 발행되지 않는다 → 응급 서비스에 유령 데이터가 가지 않는다.</li>
 *   <li>Kafka 발행 실패가 접수 등록 트랜잭션을 되돌리지 않는다.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "emergency.kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class EmergencyReceptionKafkaEventListener {

    private static final Logger log = LoggerFactory.getLogger(EmergencyReceptionKafkaEventListener.class);

    private final EmergencyReceptionEventProducer producer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(EmergencyReceptionRegisteredInternalEvent snapshot) {
        try {
            producer.send(ReceptionIntakeEvent.from(snapshot));
        } catch (Exception e) {
            // send() 내부의 비동기 실패는 Producer 에서 로깅한다.
            // 여기서 잡히는 건 직렬화/매핑 등 동기 단계 오류다.
            log.error("응급 접수 이벤트 생성/전송 시작 실패 - receptionId={}", snapshot.receptionId(), e);
        }
    }
}
