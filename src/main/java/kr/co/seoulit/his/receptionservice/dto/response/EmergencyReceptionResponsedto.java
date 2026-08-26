package kr.co.seoulit.his.receptionservice.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 응급접수 등록 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyReceptionResponsedto {

    /** 접수 ID */
    private Long receptionId;

    /** 환자 ID (CB2 UUID) */
    private String patientId;

    /** 진료과 ID */
    private Long deptId;

    /** 담당 의사 ID */
    private String doctorId;

    /** 접수번호 */
    private String receptionNo;

    /** 접수유형 */
    private String receptionType;

    /** 접수상태 */
    private String status;

    /** 메모 */
    private String memo;

    /** 접수일시 */
    private LocalDateTime receptionDate;

    /** KTAS 등급 */
    private Integer ktasLevel;

    /** 내원방법 */
    private String visitMethod;

    /** 주호소 */
    private String chiefComplaint;

    /** 의식상태 */
    private String consciousness;

    /** 중증도분류(triage) 일시 */
    private LocalDateTime triageDateTime;
}
