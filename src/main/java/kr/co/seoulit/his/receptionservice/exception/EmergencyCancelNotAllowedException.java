package kr.co.seoulit.his.receptionservice.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * 응급에서 이미 진료 기록(처치·투약·KTAS·병상 배정 등)이 생긴 접수를 취소하려 할 때 발생하는 예외 (RCP009)
 * - 응급은 이런 접수의 취소 이벤트를 받아도 취소하지 않고 진료 중으로 둔다. 접수에서 먼저 막아
 *   접수(취소)와 응급(진료 중) 상태가 어긋나지 않게 한다.
 */
public class EmergencyCancelNotAllowedException extends ReceptionException {

    public EmergencyCancelNotAllowedException(String receptionId, List<String> records) {
        super(HttpStatus.CONFLICT, "RCP009",
                "응급에서 진료가 시작된 접수라 취소할 수 없습니다. receptionId=" + receptionId + ", records=" + records);
    }
}
