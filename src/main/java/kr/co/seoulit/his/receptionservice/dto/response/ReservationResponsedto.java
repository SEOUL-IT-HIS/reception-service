package kr.co.seoulit.his.receptionservice.dto.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 예약 응답 DTO (예약 목록용)
 * - 환자명은 채우지 않는다 (프론트가 patientId 로 CB2 batch 조회해서 직접 조합).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponsedto {

    private String reservationId;

    private String patientId;

    private String deptId;

    /** 진료과명 (공통코드 캐시) */
    private String deptName;

    private String doctorId;

    /** 담당 의사명 (admin-service 조회 결과) */
    private String doctorName;

    /** 초진/재진 — INITIAL / REVISIT */
    private String visitType;

    private LocalDate reservationDate;

    /** HH:mm */
    private String reservationTime;

    private String memo;

    /** RESERVED / RECEIVED */
    private String status;

    /** 접수하기로 만들어진 접수ID (접수 전에는 null) */
    private String receptionId;
}
