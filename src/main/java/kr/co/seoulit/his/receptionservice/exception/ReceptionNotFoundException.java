package kr.co.seoulit.his.receptionservice.exception;

import org.springframework.http.HttpStatus;

/**
 * 접수 정보를 찾을 수 없을 때 발생하는 예외 (RCP001)
 */
public class ReceptionNotFoundException extends ReceptionException {

    public ReceptionNotFoundException(Long receptionId) {
        super(HttpStatus.NOT_FOUND, "RCP001", "접수 정보를 찾을 수 없습니다. receptionId=" + receptionId);
    }
}
