package kr.co.seoulit.his.receptionservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import kr.co.seoulit.his.receptionservice.common.ApiResponse;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * 전역 예외 처리기
 * 개발표준가이드 11.3/11.5에 따라 모든 에러 응답도 {code, message, data(null)} 포맷으로 내려준다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ReceptionException.class)
    public ResponseEntity<ApiResponse<Void>> handleReceptionException(ReceptionException e) {
        return ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error(e.getHttpStatus().value(), e.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "RCP004"));
    }

    /**
     * 요청 본문(JSON) 파싱/타입 실패 — 어느 필드가 어떤 타입을 기대했는지 메시지에 담아 내려준다.
     * (예: patientId 에 Long 대신 UUID 문자열이 들어온 경우 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        String message = describeJacksonCause(e.getCause());
        log.warn("RCP001 요청 본문 오류: {}", message, e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * 경로 변수/쿼리 파라미터 타입 실패
     * (예: /api/reception/{receptionId} 에 숫자가 아닌 값이 들어온 경우)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String expectedType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "알 수 없음";
        String message = String.format(
                "요청값이 올바르지 않습니다. 파라미터: %s, 입력값: %s, 기대 타입: %s",
                e.getName(), e.getValue(), expectedType);
        log.warn("RCP002 파라미터 타입 오류: {}", message, e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    private String describeJacksonCause(Throwable cause) {
        if (cause instanceof InvalidFormatException ife) {
            String field = fieldNameOf(ife);
            String expectedType = ife.getTargetType() != null ? ife.getTargetType().getSimpleName() : "알 수 없음";
            return String.format(
                    "요청값이 올바르지 않습니다. 필드: %s, 입력값: %s, 기대 타입: %s",
                    field, ife.getValue(), expectedType);
        }
        if (cause instanceof MismatchedInputException mie) {
            String field = fieldNameOf(mie);
            String expectedType = mie.getTargetType() != null ? mie.getTargetType().getSimpleName() : "알 수 없음";
            return String.format("요청값이 올바르지 않습니다. 필드: %s, 기대 타입: %s", field, expectedType);
        }
        return "요청 본문 형식이 올바르지 않습니다.";
    }

    private String fieldNameOf(tools.jackson.core.JacksonException e) {
        if (e.getPath().isEmpty()) {
            return "요청 본문";
        }
        String propertyName = e.getPath().get(e.getPath().size() - 1).getPropertyName();
        return propertyName != null ? propertyName : "요청 본문";
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("RCP999 미분류 서버 오류", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "RCP999"));
    }
}
