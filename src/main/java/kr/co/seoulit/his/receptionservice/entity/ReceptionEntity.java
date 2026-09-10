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
 * 접수(RECEPTION) 테이블 Entity
 */
@Entity
@Table(name = "RECEPTION")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionEntity {

    /** 접수ID (PK, UUID) */
    @Id
    @Column(name = "RECEPTION_ID", length = 36)
    private String receptionId;

    /** 환자ID (CB2 UUID) */
    @Column(name = "PATIENT_ID", nullable = false, length = 36)
    private String patientId;

    /** 진료과ID (DEPT_CD 공통코드의 codeValue) */
    @Column(name = "DEPT_ID", length = 36)
    private String deptId;

    /** 의사ID */
    @Column(name = "DOCTOR_ID", length = 36)
    private String doctorId;

    /** 접수번호 */
    @Column(name = "RECEPTION_NO")
    private String receptionNo;

    /** 접수유형 (예: 외래, 응급 등) */
    @Column(name = "RECEPTION_TYPE")
    private String receptionType;

    /** 접수상태 (예: 접수, 진료중, 완료, 취소 등) */
    @Column(name = "STATUS")
    private String status;

    /** 방문목적 */
    @Column(name = "VISIT_PURPOSE")
    private String memo;

    /** 접수일시 */
    @Column(name = "RECEPTION_DATE")
    private LocalDateTime receptionDate;

    /** 등록일시 */
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    /** 수정일시 */
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    /**
     * 접수상태 변경
     */
    public void changeStatus(String newStatus, LocalDateTime updatedAt) {
        this.status = newStatus;
        this.updatedAt = updatedAt;
    }
}
