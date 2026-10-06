package kr.co.seoulit.his.receptionservice.exception;

import org.springframework.http.HttpStatus;

/**
 * 예약으로 접수하거나 예약을 취소할 수 없을 때 발생하는 예외 (RCP013)
 * - 이미 접수·취소된 예약이거나, 접수 요청의 환자가 예약의 환자와 다른 경우.
 */
public class ReservationNotReceivableException extends ReceptionException {

    public ReservationNotReceivableException(String reservationId, String reason) {
        super(HttpStatus.CONFLICT, "RCP013",
                "이 예약으로는 처리할 수 없습니다. reservationId=" + reservationId + ", reason=" + reason);
    }
}
