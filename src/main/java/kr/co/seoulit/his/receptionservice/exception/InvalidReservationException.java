package kr.co.seoulit.his.receptionservice.exception;

import org.springframework.http.HttpStatus;

/**
 * 예약 등록 요청 값이 잘못됐을 때 발생하는 예외 (RCP011)
 * - 필수값 누락, 초진/재진 값 오류, 지난 날짜, 시간 형식 오류 등.
 */
public class InvalidReservationException extends ReceptionException {

    public InvalidReservationException(String detailMessage) {
        super(HttpStatus.BAD_REQUEST, "RCP011", detailMessage);
    }
}
