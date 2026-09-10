package kr.co.seoulit.his.receptionservice.dto.response;

import java.time.LocalDateTime;

//import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 접수 목록조회 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionResponsedto {

    /** 접수 ID */
    private String receptionId;

    /** 접수번호 */
    private String receptionNo;

    /** 환자 ID (CB2 UUID) */
    private String patientId;

    /** 진료과 ID */
    private String deptId;

    /** 진료과명 (admin-service 조회 결과로 채움) */
    private String deptName;

    /** 담당 의사 ID */
    private String doctorId;

    /** 담당 의사명 (admin-service 조회 결과로 채움) */
    private String doctorName;

    /** 접수유형 */
    private String receptionType;

    /** 접수상태 */
    private String status;

    /** 접수일시 */
    private LocalDateTime receptionDate;
}
