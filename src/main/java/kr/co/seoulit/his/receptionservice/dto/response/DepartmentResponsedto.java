package kr.co.seoulit.his.receptionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * admin-service GET /api/departments 응답 data[] 항목과 필드를 맞춘다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResponsedto {

    /** 진료과 ID (DEPT_CD 공통코드의 codeValue) */
    private String deptId;

    /** 진료과명 */
    private String deptName;

}
