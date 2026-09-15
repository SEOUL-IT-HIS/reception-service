package kr.co.seoulit.his.receptionservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 개발환경 편의를 위한 토픽 자동생성.
 *
 * <p>각 토픽은 해당 도메인의 {@code *.kafka.enabled} / {@code *.kafka.auto-create-topic} 이
 * 둘 다 {@code true} 일 때만 생성된다. 앱 기동 시 브로커에 토픽이 없으면 생성하고,
 * 브로커에 접속하지 못하면 로그만 남기고 기동은 계속된다(KafkaAdmin 기본 동작).
 * 운영에서는 파티션/복제 계수를 인프라에서 직접 정하고 이 옵션들을 false 로 둔다.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    @ConditionalOnProperty(
            name = {"outpatient.kafka.enabled", "outpatient.kafka.auto-create-topic"},
            havingValue = "true",
            matchIfMissing = false)
    public NewTopic outpatientReceptionTopic(@Value("${outpatient.kafka.topic}") String topic) {
        return TopicBuilder.name(topic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    @ConditionalOnProperty(
            name = {"emergency.kafka.enabled", "emergency.kafka.auto-create-topic"},
            havingValue = "true",
            matchIfMissing = false)
    public NewTopic emergencyReceptionTopic(@Value("${emergency.kafka.topic}") String topic) {
        return TopicBuilder.name(topic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
