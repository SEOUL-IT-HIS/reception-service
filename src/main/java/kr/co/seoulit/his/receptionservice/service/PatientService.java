package kr.co.seoulit.his.receptionservice.service;

import java.util.List;

import kr.co.seoulit.his.receptionservice.dto.response.PatientDetailResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.PatientSummaryResponsedto;

public interface PatientService {

    /**
     * 환자 목록 조회
     */
    List<PatientSummaryResponsedto> getPatientList();

    /**
     * 환자 상세 조회
     */
    PatientDetailResponsedto getPatient(String patientId);

}