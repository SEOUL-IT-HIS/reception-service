package kr.co.seoulit.his.receptionservice.exception;

import org.springframework.http.HttpStatus;

/**
 * 접수유형이 응급인데 응급접수정보가 없을 때 발생하는 예외 (RCP003)
 */
public class EmergencyInfoRequiredException extends ReceptionException {

    public EmergencyInfoRequiredException() {
        super(HttpStatus.BAD_REQUEST, "RCP003", "접수유형이 응급인 경우 응급접수정보는 필수입니다.");
    }
}
