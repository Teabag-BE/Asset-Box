package io.teabag.assetbox.common.dto;

public record ApiResponse<T>(
        boolean success,
        String successMessage,
        T data,
        ErrorBody error
) {

    public static <T> ApiResponse<T> ok(T data, String successMessage) {

        return new ApiResponse<>(
                true,
                successMessage,
                data,
                null
        );
    }

    public static <T> ApiResponse<T> created(T data, String successMessage) {
        return ok(
                data,
                successMessage
        );
    }

    public static ApiResponse<Void> ok() {

        return ok(null,"");
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(
                false,
                "해당 요청이 실패되었습니다." ,
                null,
                new ErrorBody(code, message)
        );
    }

    public record ErrorBody(String code, String message) {
    }
}
