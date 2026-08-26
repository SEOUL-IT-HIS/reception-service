package kr.co.seoulit.his.receptionservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 접수 취소 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionCancelRequestdto {

    /** 취소사유코드 */
    private String cancelReasonCode;

    /** 취소사유 상세 */
    private String cancelReasonDetail;

    /** 취소자 */
    private String cancelledBy;
}
