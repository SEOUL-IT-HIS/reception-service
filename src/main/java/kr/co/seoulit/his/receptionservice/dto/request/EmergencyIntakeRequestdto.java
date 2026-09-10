package kr.co.seoulit.his.receptionservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 응급 서비스(emergency-service)로 전달하는 "응급 접수 접수내역" 요청 본문.
 *
 * <p>POST {emergency.url}/api/emergency/care/reception-intakes 로 보낸다.
 * 응급 서비스가 요구하는 필드/이름과 1:1로 일치하므로, 변경 시 반드시 응급 측과 합의한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyIntakeRequestdto {

    /** 접수번호 */
    private String receptionNo;

    /** 환자 ID (CB2 UUID) */
    private String patientId;

    /** 환자명 */
    private String patientName;

    /** 내원경로(내원방법) */
    private String arrivalPath;

    /** 접수일시 (ISO-8601 문자열) */
    private String receivedAt;

    /** 주호소 원문 */
    private String chiefComplaintRaw;
}
