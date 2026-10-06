package kr.co.seoulit.his.receptionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 환자의 외래 진료 이력으로 판정한 초진/재진.
 * 외래 서비스 장애/타임아웃, 또는 기능이 꺼져 있으면 determined=false, visitType=null 로 응답한다
 * (이 경우 화면은 접수 담당자가 직접 선택하게 한다 — 재진으로 간주하지 않는다).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitTypeResponsedto {

    /** INITIAL(초진) / REVISIT(재진). 판정하지 못했으면 null */
    private String visitType;

    /** 외래 진료 이력으로 판정했는지 여부 */
    private boolean determined;

    /** 가장 최근 외래 진료일 (yyyy-MM-dd, 이력이 없거나 판정 못 했으면 null) */
    private String lastVisitDate;
}
