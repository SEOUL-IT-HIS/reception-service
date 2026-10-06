package kr.co.seoulit.his.receptionservice.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예약(RESERVATION) 테이블 Entity
 *
 * <p>예약 목록에서 "접수하기"로 접수가 만들어지면 STATUS 가 RECEIVED 가 되고 RECEPTION_ID 에 접수ID가 들어간다.
 */
@Entity
@Table(name = "RESERVATION")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationEntity {

    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_RECEIVED = "RECEIVED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    /** 예약ID (PK, UUID) */
    @Id
    @Column(name = "RESERVATION_ID", length = 36)
    private String reservationId;

    /** 환자ID (CB2 UUID) */
    @Column(name = "PATIENT_ID", nullable = false, length = 36)
    private String patientId;

    /** 진료과ID (DEPT_CD 공통코드의 codeValue) */
    @Column(name = "DEPT_ID", nullable = false, length = 36)
    private String deptId;

    /** 담당의 (ADM EMPLOYEE.EMP_ID) */
    @Column(name = "DOCTOR_ID", nullable = false, length = 36)
    private String doctorId;

    /** 초진/재진 — INITIAL / REVISIT */
    @Column(name = "VISIT_TYPE", nullable = false, length = 20)
    private String visitType;

    /** 예약일 */
    @Column(name = "RESERVATION_DATE", nullable = false)
    private LocalDate reservationDate;

    /** 예약 시간 (HH:mm) */
    @Column(name = "RESERVATION_TIME", nullable = false, length = 5)
    private String reservationTime;

    /** 방문목적 (접수로 옮겨질 때 접수의 방문목적이 된다) */
    @Column(name = "MEMO", length = 200)
    private String memo;

    /** 예약상태 — RESERVED / RECEIVED / CANCELLED */
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    /** 접수하기로 만들어진 접수ID (접수 전에는 null) */
    @Column(name = "RECEPTION_ID", length = 36)
    private String receptionId;

    /** 등록일시 */
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    /** 수정일시 */
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    /** 예약 취소 처리 — 접수되지 않은(RESERVED) 예약만 취소할 수 있다. */
    public void cancel(LocalDateTime now) {
        this.status = STATUS_CANCELLED;
        this.updatedAt = now;
    }

    /** 접수 완료 처리 — 만들어진 접수ID를 연결한다. */
    public void markReceived(String receptionId, LocalDateTime now) {
        this.status = STATUS_RECEIVED;
        this.receptionId = receptionId;
        this.updatedAt = now;
    }
}
