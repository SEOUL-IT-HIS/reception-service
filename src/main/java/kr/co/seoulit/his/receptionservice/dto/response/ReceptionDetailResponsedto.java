package kr.co.seoulit.his.receptionservice.dto.response;

import java.time.LocalDateTime;
// import java.util.List;

// import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 접수 상세조회 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionDetailResponsedto {

    /** 접수 ID */
    private Long receptionId;

    /** 환자 ID (CB2 UUID) */
    private String patientId;

    /** 진료과 ID */
    private Long deptId;

    /** 진료과명 (공통코드 캐시 DEPT_CD 로 채움) */
    private String deptName;

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

    /** 등록일시 */
    private LocalDateTime createdAt;

    /** 수정일시 */
    private LocalDateTime updatedAt;
}