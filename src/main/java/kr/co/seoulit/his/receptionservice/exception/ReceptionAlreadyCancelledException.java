package kr.co.seoulit.his.receptionservice.exception;

import org.springframework.http.HttpStatus;

/**
 * 이미 취소된 접수를 다시 취소하려 할 때 발생하는 예외 (RCP002)
 */
public class ReceptionAlreadyCancelledException extends ReceptionException {

    public ReceptionAlreadyCancelledException(Long receptionId) {
        super(HttpStatus.CONFLICT, "RCP002", "이미 취소된 접수입니다. receptionId=" + receptionId);
    }
}
