package kr.co.seoulit.his.receptionservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 응급접수 등록 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyReceptionRequestdto {

    /** 환자 ID (CB2 UUID) */
    private String patientId;

    /** 진료과 ID */
    private Long deptId;

    /** 담당 의사 ID */
    private String doctorId;

    /** 접수 메모 */
    private String memo;

    /** KTAS 등급 */
    private Integer ktasLevel;

    /** 내원방법 */
    private String visitMethod;

    /** 주호소 */
    private String chiefComplaint;

    /** 의식상태 */
    private String consciousness;
}
