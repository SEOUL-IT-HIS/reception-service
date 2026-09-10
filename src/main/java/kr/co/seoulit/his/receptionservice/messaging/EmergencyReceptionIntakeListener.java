package kr.co.seoulit.his.receptionservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import kr.co.seoulit.his.receptionservice.delegate.EmergencyBusinessDelegate;
import kr.co.seoulit.his.receptionservice.dto.request.EmergencyIntakeRequestdto;
import kr.co.seoulit.his.receptionservice.messaging.event.EmergencyReceptionRegisteredInternalEvent;
import lombok.RequiredArgsConstructor;

/**
 * ER 접수 저장 트랜잭션이 <b>정상 커밋된 후에만</b> 응급 서비스로 접수내역을 REST 전송한다.
 *
 * <ul>
 *   <li>트랜잭션이 롤백되면 전송도 하지 않는다 → 응급 서비스에 유령 데이터가 가지 않는다.</li>
 *   <li>전송 실패(응급 서비스 장애/타임아웃)가 접수 등록을 되돌리지 않는다 → 로그만 남긴다 (fire-and-forget).</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "emergency.intake.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class EmergencyReceptionIntakeListener {

    private static final Logger log = LoggerFactory.getLogger(EmergencyReceptionIntakeListener.class);

    private final EmergencyBusinessDelegate emergencyBusinessDelegate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(EmergencyReceptionRegisteredInternalEvent snapshot) {
        try {
            emergencyBusinessDelegate.sendReceptionIntake(EmergencyIntakeRequestdto.builder()
                    .receptionNo(snapshot.receptionNo())
                    .patientId(snapshot.patientId())
                    .patientName(snapshot.patientName())
                    .arrivalPath(snapshot.arrivalPath())
                    .receivedAt(snapshot.receivedAt() != null ? snapshot.receivedAt().toString() : null)
                    .chiefComplaintRaw(snapshot.chiefComplaintRaw())
                    .build());
            log.info("응급 서비스 접수내역 전달 완료 - receptionNo={}", snapshot.receptionNo());
        } catch (Exception e) {
            log.error("응급 서비스 접수내역 전달 실패 - receptionNo={} (접수는 정상 처리됨)",
                    snapshot.receptionNo(), e);
        }
    }
}
