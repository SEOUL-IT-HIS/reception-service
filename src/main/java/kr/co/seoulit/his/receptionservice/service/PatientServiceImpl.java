package kr.co.seoulit.his.receptionservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.co.seoulit.his.receptionservice.delegate.PatientBusinessDelegate;
import kr.co.seoulit.his.receptionservice.dto.response.PatientDetailResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.PatientSummaryResponsedto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientBusinessDelegate patientDelegate;

    /**
     * 환자 목록 조회
     */
    @Override
    public List<PatientSummaryResponsedto> getPatientList() {
        return patientDelegate.getPatients().data();
    }

    /**
     * 환자 상세 조회
     */
    @Override
    public PatientDetailResponsedto getPatient(String patientId) {
        return patientDelegate.getPatientById(patientId);
    }

}