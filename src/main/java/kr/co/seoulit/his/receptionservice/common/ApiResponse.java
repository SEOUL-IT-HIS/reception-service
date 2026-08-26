package kr.co.seoulit.his.receptionservice.common;

/**
 * 공통 API 응답 포맷 (개발표준가이드 11.3)
 * { "code": 200, "message": "SUCCESS", "data": {} }
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> success(int code, T data) {
        return new ApiResponse<>(code, "SUCCESS", data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
