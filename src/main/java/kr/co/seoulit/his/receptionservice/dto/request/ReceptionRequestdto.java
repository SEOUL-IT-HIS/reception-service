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

    /** 접수유형 */
    private String receptionType;

    /** 접수 메모 */
    private String memo;
}