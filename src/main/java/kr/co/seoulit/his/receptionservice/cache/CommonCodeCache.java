package kr.co.seoulit.his.receptionservice.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import kr.co.seoulit.his.receptionservice.delegate.CommonCodeBusinessDelegate;
import kr.co.seoulit.his.receptionservice.dto.response.CommonCodeGroupResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.CommonCodeItemResponsedto;
import lombok.RequiredArgsConstructor;

/**
 * admin-service 공통코드(그룹+항목)를 통째로 읽어와 메모리에 캐싱한다.
 * - 진료과(DEPT_CD)처럼 자주 조회되지만 자주 바뀌지 않는 공통코드를 매 요청마다
 *   admin-service로 호출하지 않기 위한 용도.
 * - 시작 시점에 admin-service가 응답하지 않아도 애플리케이션 자체는 뜨도록, 캐시 적재
 *   실패는 로그만 남기고 삼킨다 (그 동안은 빈 목록을 반환).
 * - 기동 시점에 admin-service가 잠깐이라도 불통이면 재시작 전까지 캐시가 영영 빈 채로
 *   굳는 문제가 있었어서, {@link #reload()}를 5분마다 자동 실행한다. admin-service가
 *   나중에 복구되면 재시작 없이도 최대 5분 내로 자연 회복된다.
 */
@Component
@RequiredArgsConstructor
public class CommonCodeCache {

    private static final Logger log = LoggerFactory.getLogger(CommonCodeCache.class);
    private static final long RELOAD_INTERVAL_MS = 5 * 60 * 1000L;

    private final CommonCodeBusinessDelegate commonCodeBusinessDelegate;

    private volatile Map<String, List<CommonCodeItemResponsedto>> itemsByGroupCode = Map.of();

    @PostConstruct
    public void load() {
        try {
            List<CommonCodeGroupResponsedto> groups = commonCodeBusinessDelegate.getGroups().data();

            Map<String, List<CommonCodeItemResponsedto>> loaded = new HashMap<>();
            int itemCount = 0;
            for (CommonCodeGroupResponsedto group : groups) {
                if (!"Y".equals(group.getUseYn())) {
                    continue;
                }
                List<CommonCodeItemResponsedto> items = commonCodeBusinessDelegate.getItems(group.getGroupId())
                        .data()
                        .stream()
                        .filter(item -> "Y".equals(item.getUseYn()))
                        .toList();
                loaded.put(group.getGroupCode(), items);
                itemCount += items.size();
            }

            this.itemsByGroupCode = loaded;
            log.info("공통코드 로컬캐시 적재 완료: {}개 그룹, {}개 항목", loaded.size(), itemCount);
        } catch (Exception e) {
            log.warn("공통코드 로컬캐시 적재 실패 - admin-service 응답 없음. 재기동 전까지 공통코드 조회는 빈 목록을 반환한다.", e);
        }
    }

    /**
     * 서버 재기동 없이 캐시를 다시 채운다.
     * 5분마다 자동 실행되어, 기동 시점에 admin-service가 불통이었어도 나중에 복구되면
     * 스스로 회복된다. (기동 직후 곧바로 다시 돌지 않도록 initialDelay 도 5분으로 둔다)
     */
    @Scheduled(initialDelay = RELOAD_INTERVAL_MS, fixedDelay = RELOAD_INTERVAL_MS)
    public void reload() {
        load();
    }

    /** 그룹코드(예: DEPT_CD) 기준 사용중(useYn=Y) 항목 목록 — 캐시에 없으면 빈 목록 */
    public List<CommonCodeItemResponsedto> getItems(String groupCode) {
        return itemsByGroupCode.getOrDefault(groupCode, List.of());
    }

}
