package kr.co.seoulit.his.receptionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * admin-service GET /api/departments/{deptId}/doctors 응답 data[] 항목과 필드를 맞춘다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponsedto {

    /** 의사 ID */
    private String doctorId;

    /** 의사명 */
    private String doctorName;

    /** 소속 진료과 ID */
    private String deptId;

}
