package kr.co.seoulit.his.receptionservice.service;

import java.util.List;

import kr.co.seoulit.his.receptionservice.common.ApiResponse;
import kr.co.seoulit.his.receptionservice.dto.request.EmergencyReceptionRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionCancelRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionStatusChangeRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReservationRequestdto;
import kr.co.seoulit.his.receptionservice.dto.response.DepartmentResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.DoctorResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.EmergencyActiveCheckResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.EmergencyReceptionResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.ReceptionDetailResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.ReceptionResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.ReservationResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.VisitTypeResponsedto;

public interface ReceptionService {

    /**
     * 접수 등록
     */
    void registerReception(ReceptionRequestdto request);

    /**
     * 환자의 외래 진료 이력으로 초진/재진을 판정한다. 외래 서비스 장애·미연동 시 판정하지 않고(determined=false) 응답한다.
     */
    VisitTypeResponsedto getVisitType(String patientId);

    /**
     * 예약 등록
     */
    void registerReservation(ReservationRequestdto request);

    /**
     * 예약 취소 — 접수되지 않은(RESERVED) 예약만 취소할 수 있다.
     */
    void cancelReservation(String reservationId);

    /**
     * 예약 목록 조회 (오늘 이후 예약)
     */
    List<ReservationResponsedto> getReservationList();

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
     * 응급접수 등록 직전, 같은 환자의 진행 중(미퇴실) 응급접수가 있는지 확인한다.
     * 응급 서비스 장애/타임아웃 시에도 등록을 막지 않도록 경고 없음(hasActiveReception=false)으로 응답한다(fail-open).
     */
    EmergencyActiveCheckResponsedto checkActiveEmergencyReception(String patientId);

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