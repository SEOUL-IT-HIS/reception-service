package kr.co.seoulit.his.receptionservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 접수 상태 변경 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionStatusChangeRequestdto {

    /** 변경할 접수상태 */
    private String newStatus;

    /** 변경자 */
    private String changedBy;

    /** 변경사유 */
    private String reason;
}
