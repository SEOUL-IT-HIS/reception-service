package kr.co.seoulit.his.receptionservice.exception;

import org.springframework.http.HttpStatus;

/**
 * 외래 접수에 담당의(doctorId)가 없거나 형식이 잘못됐을 때 발생하는 예외 (RCP006)
 * - 외래(OPD)는 접수 이벤트의 doctorId 를 DOCTOR_ID(NOT NULL, length 36)에 그대로 저장하므로
 *   비어 있거나 36자를 넘으면 외래 쪽 저장이 실패한다. 접수 단계에서 미리 막는다.
 */
public class DoctorRequiredException extends ReceptionException {

    public DoctorRequiredException(String detailMessage) {
        super(HttpStatus.BAD_REQUEST, "RCP006", detailMessage);
    }
}
