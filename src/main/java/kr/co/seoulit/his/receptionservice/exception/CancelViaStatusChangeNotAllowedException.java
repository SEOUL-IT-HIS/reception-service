package kr.co.seoulit.his.receptionservice.exception;

import org.springframework.http.HttpStatus;

/**
 * 상태 변경 API로 CANCELLED 를 넣으려 할 때 발생하는 예외 (RCP008)
 * - 취소는 취소 API(PATCH /{receptionId}/cancel)로만 한다. 상태 변경으로 바꾸면 취소 기록(RECEPTION_CANCEL)과
 *   외래/응급 취소 이벤트가 남지 않아, 상대 서비스가 취소를 모르게 된다.
 */
public class CancelViaStatusChangeNotAllowedException extends ReceptionException {

    public CancelViaStatusChangeNotAllowedException(String receptionId) {
        super(HttpStatus.BAD_REQUEST, "RCP008",
                "접수 취소는 취소 API로만 할 수 있습니다. receptionId=" + receptionId);
    }
}
