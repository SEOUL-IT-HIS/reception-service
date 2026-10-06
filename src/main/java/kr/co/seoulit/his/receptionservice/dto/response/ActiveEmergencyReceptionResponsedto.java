package kr.co.seoulit.his.receptionservice.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 응급 서비스의 "진행 중(미퇴실) 응급접수" 조회 응답 1건
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveEmergencyReceptionResponsedto {

    /** 응급 서비스 쪽 접수 ID */
    private String receptionId;

    /** 환자 ID (CB2 UUID) */
    private String patientId;

    /** 접수(내원) 일시 */
    private LocalDateTime receivedAt;
}
