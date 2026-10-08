package kr.co.seoulit.his.receptionservice.exception;

import org.springframework.http.HttpStatus;

/**
 * 외래에서 이미 진료가 시작(진행 중) 또는 끝난 접수를 취소하려 할 때 발생하는 예외 (RCP014)
 * - 외래는 이런 접수의 취소 메시지를 받아도 상태를 바꾸지 않는다. 접수에서 먼저 막아
 *   접수(취소)와 외래(진료 중/완료) 상태가 어긋나지 않게 한다.
 */
public class OutpatientCancelNotAllowedException extends ReceptionException {

    public OutpatientCancelNotAllowedException(String receptionId, String outpatientStatus) {
        super(HttpStatus.CONFLICT, "RCP014",
                "외래에서 진료가 시작된 접수라 취소할 수 없습니다. receptionId=" + receptionId + ", status=" + outpatientStatus);
    }
}
