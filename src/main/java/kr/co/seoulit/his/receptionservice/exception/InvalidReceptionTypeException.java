package kr.co.seoulit.his.receptionservice.exception;

import org.springframework.http.HttpStatus;

/**
 * 외래 접수의 접수유형(예약/당일) 또는 초진/재진 값이 허용 범위를 벗어났을 때 발생하는 예외 (RCP007)
 */
public class InvalidReceptionTypeException extends ReceptionException {

    public InvalidReceptionTypeException(String detailMessage) {
        super(HttpStatus.BAD_REQUEST, "RCP007", detailMessage);
    }
}
