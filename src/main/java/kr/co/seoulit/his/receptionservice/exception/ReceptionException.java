package kr.co.seoulit.his.receptionservice.exception;

import org.springframework.http.HttpStatus;

/**
 * 접수(reception-service) 도메인 공통 예외
 * 메시지 코드 체계(개발표준가이드 15.2)에 따라 "RCP" + 3자리 코드를 사용한다.
 */
public abstract class ReceptionException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;

    protected ReceptionException(HttpStatus httpStatus, String code, String detailMessage) {
        super(detailMessage);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /** 메시지 코드 (예: RCP001). 프론트는 features/reception/messages.ts 사전으로 문구를 변환한다. */
    public String getCode() {
        return code;
    }
}
