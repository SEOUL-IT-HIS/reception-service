package kr.co.seoulit.his.receptionservice.exception;

import org.springframework.http.HttpStatus;

/**
 * 현재 상태와 동일한 상태로 변경하려 할 때 발생하는 예외 (RCP005)
 */
public class ReceptionStatusUnchangedException extends ReceptionException {

    public ReceptionStatusUnchangedException(String receptionId, String status) {
        super(HttpStatus.CONFLICT, "RCP005",
                "현재 상태와 동일한 상태로 변경할 수 없습니다. receptionId=" + receptionId + ", status=" + status);
    }
}
