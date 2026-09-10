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
    private String receptionId;

    /** 환자 ID (CB2 UUID) */
    private String patientId;

    /** 환자명 (CB2 조회) */
    private String patientName;

    /** 진료과 ID */
    private String deptId;

    /** 진료과명 (공통코드 캐시 조회) */
    private String deptName;

    /** 담당 의사 ID */
    private String doctorId;

    /** 담당 의사명 (admin-service 조회) */
    private String doctorName;

    /** 접수번호 */
    private String receptionNo;

    /** 접수유형 */
    private String receptionType;

    /** 접수상태 */
    private String status;

    /** 메모 */
    private String memo;

    /** 접수일시 */
    private LocalDateTime receivedAt;

    /** KTAS 등급 */
    private Integer ktasLevel;

    /** 내원경로(내원방법) */
    private String arrivalPath;

    /** 주호소 원문 */
    private String chiefComplaintRaw;

    /** 의식상태 */
    private String consciousness;

    /** 중증도분류(triage) 일시 */
    private LocalDateTime triageDateTime;
}
