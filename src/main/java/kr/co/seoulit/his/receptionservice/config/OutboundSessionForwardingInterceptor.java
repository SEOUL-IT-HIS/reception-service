package kr.co.seoulit.his.receptionservice.config;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 현재 처리 중인 HTTP 요청의 인증 헤더(Cookie / Authorization)를 아웃바운드 REST 호출에 그대로 전달한다.
 *
 * <p>admin-service 등 세션 인증을 요구하는 내부 서비스를 "로그인한 사용자 대신" 호출하기 위함.
 * 요청 컨텍스트가 없는 경우(스케줄러, {@code @TransactionalEventListener(AFTER_COMMIT)} 등)엔
 * 아무것도 하지 않는다.
 */
@Component
public class OutboundSessionForwardingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        HttpServletRequest inbound = currentRequest();
        if (inbound != null) {
            copyHeader(inbound, request.getHeaders(), HttpHeaders.COOKIE);
            copyHeader(inbound, request.getHeaders(), HttpHeaders.AUTHORIZATION);
        }
        return execution.execute(request, body);
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private static void copyHeader(HttpServletRequest inbound, HttpHeaders outboundHeaders, String name) {
        if (outboundHeaders.getFirst(name) != null) {
            return;
        }
        String value = inbound.getHeader(name);
        if (value != null && !value.isBlank()) {
            outboundHeaders.set(name, value);
        }
    }
}
