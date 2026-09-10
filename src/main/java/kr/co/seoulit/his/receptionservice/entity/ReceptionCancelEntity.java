package kr.co.seoulit.his.receptionservice.entity;

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
 * 접수취소(RECEPTION_CANCEL) 테이블 Entity
 */
@Entity
@Table(name = "RECEPTION_CANCEL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionCancelEntity {

    /** 접수취소ID (PK, UUID) */
    @Id
    @Column(name = "CANCEL_ID", length = 36)
    private String cancelId;

    /** 접수ID */
    @Column(name = "RECEPTION_ID", nullable = false, length = 36)
    private String receptionId;

    /** 취소사유코드 */
    @Column(name = "CANCEL_REASON_CODE")
    private String cancelReasonCode;

    /** 취소사유 상세 */
    @Column(name = "CANCEL_REASON_DETAIL")
    private String cancelReasonDetail;

    /** 취소자 */
    @Column(name = "CANCELLED_BY")
    private String cancelledBy;

    /** 취소일시 */
    @Column(name = "CANCELLED_AT")
    private LocalDateTime cancelledAt;
}
