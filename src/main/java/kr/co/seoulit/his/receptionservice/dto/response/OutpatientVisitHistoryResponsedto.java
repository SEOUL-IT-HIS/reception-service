package kr.co.seoulit.his.receptionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 외래 서비스 GET /api/outpatient/encounters/visit-history 응답 data 와 필드를 맞춘다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutpatientVisitHistoryResponsedto {

    /** 삭제되지 않은 진료기록이 있는 외래건이 있는지 */
    private boolean hasVisitRecord;

    /** 가장 최근 진료일 (yyyy-MM-dd) 또는 null */
    private String lastVisitDate;
}
