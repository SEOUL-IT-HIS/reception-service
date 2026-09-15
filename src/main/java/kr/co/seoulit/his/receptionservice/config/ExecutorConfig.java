package kr.co.seoulit.his.receptionservice.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 접수 목록 조회 시 진료과별 의사명을 admin-service에 병렬로 물어보기 위한 스레드풀.
 *
 * <p>목록에 등장하는 진료과 수만큼 admin-service를 순차 호출하면(예: 6개 진료과 × 커넥트
 * 타임아웃 3초) 목록 하나 띄우는 데 20초 가까이 걸려 프론트 타임아웃(15초)을 넘길 수 있다.
 * 병렬로 물어보면 전체 대기시간이 "진료과 수 × 타임아웃"이 아니라 "타임아웃 1회" 수준으로 준다.
 */
@Configuration
public class ExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService adminLookupExecutor() {
        return Executors.newFixedThreadPool(8);
    }
}
