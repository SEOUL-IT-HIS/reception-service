package kr.co.seoulit.his.receptionservice.dto.request;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 예약 등록 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequestdto {

    /** 환자 ID (CB2 UUID) */
    private String patientId;

    /** 진료과 ID */
    private String deptId;

    /** 담당 의사 ID (ADM EMP_ID) */
    private String doctorId;

    /** 초진/재진 — INITIAL / REVISIT */
    private String visitType;

    /** 예약일 (yyyy-MM-dd) */
    private LocalDate reservationDate;

    /** 예약 시간 (HH:mm) */
    private String reservationTime;

    /** 방문목적 */
    private String memo;
}
