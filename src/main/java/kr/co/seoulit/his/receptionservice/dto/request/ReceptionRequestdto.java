package kr.co.seoulit.his.receptionservice.dto.request;

// import java.time.LocalDateTime;

// import io.swagger.v3.oas.annotations.media.Schema;
// import jakarta.validation.Valid;
// import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.NotNull;
// import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 접수 등록 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionRequestdto {

    /** 환자 ID (CB2 UUID) */
    private String patientId;

    /** 진료과 ID */
    private String deptId;

    /** 담당 의사 ID */
    private String doctorId;

    /** (무시됨) 접수유형은 서버가 정한다 — reservationId 가 있으면 RESERVATION(예약), 없으면 WALK_IN(당일) */
    private String receptionType;

    /** 초진/재진 — INITIAL(신규환자등록으로 선택)/REVISIT(환자검색으로 선택) */
    private String visitType;

    /** 예약 목록의 "접수하기"로 들어온 경우 그 예약ID. 있으면 접수유형이 예약(RESERVATION)이 되고 예약이 접수 완료 처리된다. */
    private String reservationId;

    /** 접수 메모 */
    private String memo;
}