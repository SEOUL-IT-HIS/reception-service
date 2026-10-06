package kr.co.seoulit.his.receptionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * admin-service GET /api/admin/emp/list 응답 data[] 항목 중 접수에서 쓰는 필드만 맞춘다.
 * (나머지 필드는 무시된다)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpResponsedto {

    /** EMPLOYEE.EMP_ID — 접수의 DOCTOR_ID 로 저장되는 값 */
    private String empId;

    /** 사번 */
    private String empNo;

    /** 직원 이름 */
    private String empName;

    /** 부서 공통코드 (DEPT_CD) */
    private String deptCode;

}
