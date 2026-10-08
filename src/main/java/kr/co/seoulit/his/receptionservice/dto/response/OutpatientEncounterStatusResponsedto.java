package kr.co.seoulit.his.receptionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 외래 서비스 GET /api/outpatient/encounters/by-reception/{receptionId}/status 응답 data 와 필드를 맞춘다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutpatientEncounterStatusResponsedto {

    private String receptionId;

    private String encounterId;

    /** WAITING(대기중) / IN_PROGRESS(진료중) / COMPLETED(진료완료) / CANCELLED(취소) */
    private String status;

    /** 의사가 진료를 시작한 상태인지 (IN_PROGRESS 일 때 true) */
    private boolean inProgress;
}
