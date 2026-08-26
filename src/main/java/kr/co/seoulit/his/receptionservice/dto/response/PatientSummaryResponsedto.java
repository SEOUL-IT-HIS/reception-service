package kr.co.seoulit.his.receptionservice.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CB2(환자서비스) GET /api/patient/list 응답 data[] 항목과 필드를 맞춘다.
 * CB2는 gender를 내려주지 않고 residentRegNo/statusCd/createdAt/updatedAt을 내려준다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientSummaryResponsedto {

    /** 환자 ID (CB2 UUID) */
    private String patientId;

    /** 환자명 */
    private String patientName;

    /** 마스킹된 주민등록번호 (예: 000813-4******) */
    private String residentRegNo;

    /** 생년월일 */
    private LocalDate birthDate;

    /** 환자 상태 코드 */
    private String statusCd;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}