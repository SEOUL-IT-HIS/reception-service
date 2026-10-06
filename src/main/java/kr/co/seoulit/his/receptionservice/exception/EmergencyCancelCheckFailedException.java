package kr.co.seoulit.his.receptionservice.exception;

import org.springframework.http.HttpStatus;

/**
 * 응급 접수 취소 전에 응급의 취소 가능 여부를 확인하지 못했을 때 발생하는 예외 (RCP010)
 * - 응급 서비스 장애·타임아웃, 또는 응급이 CANNOT_VERIFY(처방 여부 확인 불가)로 응답한 경우.
 * - 확인 없이 취소하면 응급과 상태가 어긋날 수 있으므로 취소를 막고 잠시 후 다시 시도하게 한다 (fail-closed).
 */
public class EmergencyCancelCheckFailedException extends ReceptionException {

    public EmergencyCancelCheckFailedException(String receptionId, String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "RCP010",
                "응급 취소 가능 여부를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요. receptionId=" + receptionId
                        + ", detail=" + detail);
    }
}
