package kr.co.seoulit.his.receptionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * admin-service GET /api/commonCodeItem/list?groupId= 응답 data[] 항목과 필드를 맞춘다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonCodeItemResponsedto {

    private String codeId;

    private String groupId;

    private String codeValue;

    private String codeName;

    private String useYn;

}
