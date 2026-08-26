package kr.co.seoulit.his.receptionservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
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

    /** 응급접수정보ID (PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_EMERGENCY_INFO_GENERATOR")
    @SequenceGenerator(
            name = "SEQ_EMERGENCY_INFO_GENERATOR",
            sequenceName = "SEQ_EMERGENCY_INFO",
            allocationSize = 1)
    @Column(name = "EMERGENCY_INFO_ID")
    private Long emergencyInfoId;

    /** 접수ID (FK) */
    @Column(name = "RECEPTION_ID", nullable = false)
    private Long receptionId;

    /** KTAS 등급 */
    @Column(name = "KTAS_LEVEL")
    private Integer ktasLevel;

    /** 내원방법 */
    @Column(name = "VISIT_METHOD")
    private String visitMethod;

    /** 주호소 */
    @Column(name = "CHIEF_COMPLAINT")
    private String chiefComplaint;

    /** 의식상태 */
    @Column(name = "CONSCIOUSNESS")
    private String consciousness;

    /** 중증도분류(triage) 일시 */
    @Column(name = "TRIAGE_DATETIME")
    private LocalDateTime triageDateTime;
}
