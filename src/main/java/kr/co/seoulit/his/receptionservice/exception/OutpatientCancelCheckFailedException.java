package kr.co.seoulit.his.receptionservice.exception;

import org.springframework.http.HttpStatus;

/**
 * 외래 접수 취소 전에 외래의 진료 상태를 확인하지 못했을 때 발생하는 예외 (RCP015)
 * - 외래 서비스 장애·타임아웃·응답 오류.
 * - 확인 없이 취소하면 외래와 상태가 어긋날 수 있으므로 취소를 막고 잠시 후 다시 시도하게 한다 (fail-closed).
 */
public class OutpatientCancelCheckFailedException extends ReceptionException {

    public OutpatientCancelCheckFailedException(String receptionId, String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "RCP015",
                "외래 진료 상태를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요. receptionId=" + receptionId + ", detail=" + detail);
    }
}
