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
 * 응급접수정보(EMERGENCY_INFO) 테이블 Entity
 */
@Entity
@Table(name = "EMERGENCY_INFO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyInfoEntity {

    /** 응급접수정보ID (PK, UUID) */
    @Id
    @Column(name = "EMERGENCY_ID", length = 36)
    private String emergencyInfoId;

    /** 접수ID (FK) */
    @Column(name = "RECEPTION_ID", nullable = false, length = 36)
    private String receptionId;

    /** KTAS 등급 */
    @Column(name = "KTAS_LEVEL")
    private Integer ktasLevel;

    /** 내원방법 */
    @Column(name = "ARRIVAL_METHOD")
    private String visitMethod;

    /** 주호소 */
    @Column(name = "CHIEF_COMPLAINT")
    private String chiefComplaint;

    /** 의식상태 */
    @Column(name = "CONSCIOUSNESS_LEVEL")
    private String consciousness;

    /** 중증도분류(triage) 일시 */
    @Column(name = "TRAGE_TIME")
    private LocalDateTime triageDateTime;
}
