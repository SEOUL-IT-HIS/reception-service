package kr.co.seoulit.his.receptionservice.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 응급접수 중복(활성) 확인 응답.
 * 응급 서비스 장애/타임아웃 시에도 hasActiveReception=false, activeReceptions=[] 로 응답한다(fail-open).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyActiveCheckResponsedto {

    /** 같은 환자의 진행 중(미퇴실) 응급접수 존재 여부 */
    private boolean hasActiveReception;

    /** 진행 중인 응급접수 목록 (접수일시 오름차순, 없으면 빈 배열) */
    private List<ActiveEmergencyReceptionResponsedto> activeReceptions;
}
