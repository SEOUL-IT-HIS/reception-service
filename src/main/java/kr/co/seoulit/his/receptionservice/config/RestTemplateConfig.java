package kr.co.seoulit.his.receptionservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            OutboundSessionForwardingInterceptor sessionForwardingInterceptor,
            @Value("${cb2.connect-timeout}") long connectTimeout,
            @Value("${cb2.read-timeout}") long readTimeout) {
        return builder
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .readTimeout(Duration.ofMillis(readTimeout))
                .additionalInterceptors(sessionForwardingInterceptor)
                .build();
    }

    /**
     * 응급접수 중복(활성) 확인 전용 RestTemplate.
     * 다른 MSA 호출과 타임아웃을 공유하면 전체가 2초로 줄어버리므로 별도 빈으로 분리한다.
     * 등록 자체를 막으면 안 되는 호출이라 짧게(기본 2초) 잡는다 — 이 시간 안에 응답이 없으면
     * fail-open(경고 없이 그대로 진행)으로 처리한다.
     */
    @Bean
    @Qualifier("emergencyActiveCheckRestTemplate")
    public RestTemplate emergencyActiveCheckRestTemplate(
            RestTemplateBuilder builder,
            @Value("${emergency.active-check.timeout}") long timeout) {
        return builder
                .connectTimeout(Duration.ofMillis(timeout))
                .readTimeout(Duration.ofMillis(timeout))
                .build();
    }

}
