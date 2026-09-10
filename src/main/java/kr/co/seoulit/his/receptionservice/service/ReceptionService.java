package kr.co.seoulit.his.receptionservice.service;

import java.util.List;

import kr.co.seoulit.his.receptionservice.common.ApiResponse;
import kr.co.seoulit.his.receptionservice.dto.request.EmergencyReceptionRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionCancelRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionStatusChangeRequestdto;
import kr.co.seoulit.his.receptionservice.dto.response.DepartmentResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.DoctorResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.EmergencyReceptionResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.ReceptionDetailResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.ReceptionResponsedto;

public interface ReceptionService {

    /**
     * 접수 등록
     */
    void registerReception(ReceptionRequestdto request);

    /**
     * 접수 목록 조회
     */
    List<ReceptionResponsedto> getReceptionList();

    /**
     * 접수 상세 조회
     */
    ReceptionDetailResponsedto getReception(String receptionId);

    /**
     * 응급접수 등록
     */
    EmergencyReceptionResponsedto registerEmergencyReception(EmergencyReceptionRequestdto request);

    /**
     * 응급접수 목록 조회 (당일 건, 취소 제외)
     */
    List<EmergencyReceptionResponsedto> getEmergencyReceptionList();

    /**
     * 접수 상태 변경
     */
    void changeReceptionStatus(String receptionId, ReceptionStatusChangeRequestdto request);

    /**
     * 접수 취소
     */
    void cancelReception(String receptionId, ReceptionCancelRequestdto request);

    /**
     * 진료과 목록 조회
     */
    ApiResponse<List<DepartmentResponsedto>> getDepartments();

    /**
     * 진료과별 의사 목록 조회
     */
    ApiResponse<List<DoctorResponsedto>> getDoctorsByDepartment(String deptId);

}