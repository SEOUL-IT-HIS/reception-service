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
 * 접수상태변경이력(RECEPTION_STATUS_HISTORY) 테이블 Entity
 */
@Entity
@Table(name = "RECEPTION_STATUS_HISTORY")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionStatusHistoryEntity {

    /** 상태변경이력ID (PK, UUID) */
    @Id
    @Column(name = "HISTORY_ID", length = 36)
    private String historyId;

    /** 접수ID */
    @Column(name = "RECEPTION_ID", nullable = false, length = 36)
    private String receptionId;

    /** 변경 전 상태 */
    @Column(name = "PREV_STATUS")
    private String prevStatus;

    /** 변경 후 상태 */
    @Column(name = "NEW_STATUS")
    private String newStatus;

    /** 변경자 */
    @Column(name = "CHANGED_BY")
    private String changedBy;

    /** 변경일시 */
    @Column(name = "CHANGED_AT")
    private LocalDateTime changedAt;

    /** 변경사유 */
    @Column(name = "REASON")
    private String reason;
}
