package kr.co.seoulit.his.receptionservice.exception;

import org.springframework.http.HttpStatus;

/**
 * 예약 정보를 찾을 수 없을 때 발생하는 예외 (RCP012)
 */
public class ReservationNotFoundException extends ReceptionException {

    public ReservationNotFoundException(String reservationId) {
        super(HttpStatus.NOT_FOUND, "RCP012", "예약 정보를 찾을 수 없습니다. reservationId=" + reservationId);
    }
}
