package kr.co.seoulit.his.receptionservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

import kr.co.seoulit.his.receptionservice.messaging.event.ReceptionRegisteredEvent;
import kr.co.seoulit.his.receptionservice.messaging.event.ReceptionRegisteredInternalEvent;
import lombok.RequiredArgsConstructor;

/**
 * 접수 저장 트랜잭션이 <b>정상 커밋된 후에만</b> 외래로 Kafka 이벤트를 발행한다.
 *
 * <p>서비스에서 {@code kafkaTemplate.send()} 를 직접 호출하지 않고 이 리스너를 거치는 이유:
 * <ul>
 *   <li>트랜잭션이 롤백되면 이벤트도 발행되지 않는다 → 외래에 유령 데이터가 가지 않는다.</li>
 *   <li>Kafka 발행 실패가 접수 등록 트랜잭션을 되돌리지 않는다.</li>
 * </ul>
 * 발행 실패 시 유실 방지가 더 필요하면 이후 Transactional Outbox 패턴으로 확장한다.
 */
@Component
@ConditionalOnProperty(name = "outpatient.kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ReceptionRegisteredEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReceptionRegisteredEventListener.class);

    private final OutpatientReceptionEventProducer producer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ReceptionRegisteredInternalEvent snapshot) {
        try {
            producer.send(ReceptionRegisteredEvent.from(snapshot));
        } catch (Exception e) {
            // send() 내부의 비동기 실패는 Producer 에서 로깅한다.
            // 여기서 잡히는 건 직렬화/매핑 등 동기 단계 오류다.
            log.error("외래 접수 이벤트 생성/전송 시작 실패 - receptionId={}", snapshot.receptionId(), e);
        }
    }
}
