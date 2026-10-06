package kr.co.seoulit.his.receptionservice.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 응급 서비스 GET /api/emergency/care/reception-intakes/cancellable 응답 data 와 필드를 맞춘다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyCancellableResponsedto {

    /** 취소 가능 여부 */
    private boolean cancellable;

    /** CANCELLABLE / ALREADY_CANCELLED / NOT_FOUND (취소 가능), HAS_RECORDS / CANNOT_VERIFY (취소 불가) */
    private String reasonCode;

    /** HAS_RECORDS 일 때 진료 기록 종류 목록 */
    private List<String> records;
}
