package kr.co.seoulit.his.receptionservice.config;

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
            @Value("${cb2.connect-timeout}") long connectTimeout,
            @Value("${cb2.read-timeout}") long readTimeout) {
        return builder
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .readTimeout(Duration.ofMillis(readTimeout))
                .build();
    }

}
