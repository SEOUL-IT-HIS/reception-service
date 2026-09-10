package kr.co.seoulit.his.receptionservice.messaging.event;

import java.time.LocalDateTime;

import kr.co.seoulit.his.receptionservice.entity.ReceptionEntity;

/**
 * 접수 저장 직후 서비스가 발행하는 스프링 내부 이벤트(트랜잭션 경계 안).
 *
 * <p>{@code ReceptionEntity} 대신 필요한 값만 스냅샷으로 담는다.
 * 실제 Kafka 발행은 트랜잭션 커밋 이후({@code @TransactionalEventListener(AFTER_COMMIT)})에
 * 이루어지므로, 커밋 시점에 엔티티 상태에 의존하지 않도록 하기 위함이다.
 */
public record ReceptionRegisteredInternalEvent(
        String receptionId,
        String patientId,
        String deptId,
        String doctorId,
        LocalDateTime receptionDate,
        String status,
        String visitReason
) {

    public static ReceptionRegisteredInternalEvent from(ReceptionEntity reception) {
        return new ReceptionRegisteredInternalEvent(
                reception.getReceptionId(),
                reception.getPatientId(),
                reception.getDeptId(),
                reception.getDoctorId(),
                reception.getReceptionDate(),
                reception.getStatus(),
                reception.getMemo()
        );
    }
}
