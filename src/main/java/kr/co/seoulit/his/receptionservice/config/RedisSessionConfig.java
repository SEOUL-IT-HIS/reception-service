package kr.co.seoulit.his.receptionservice.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

@Configuration
public class RedisSessionConfig {

    /** "@class" 에 적힌 클래스를 되살릴 때 허용할 범위 (SessionUser 가 이 아래 있다) */
    private static final String ALLOWED_PACKAGE = "kr.co.seoulit.his.";

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        // 우리 패키지만 허용한다.
        // java.lang. 을 열어두면 ProcessBuilder 같은 위험한 클래스까지 들어온다.
        PolymorphicTypeValidator allowedTypes = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(ALLOWED_PACKAGE)
                .build();

        return GenericJacksonJsonRedisSerializer.builder()
                .customize(builder -> builder.activateDefaultTyping(
                        allowedTypes,
                        DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY))
                .build();
    }
}
