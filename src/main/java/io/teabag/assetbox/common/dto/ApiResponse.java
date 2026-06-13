package io.teabag.assetbox.common.dto;

public record ApiResponse<T>(
        boolean success,
        String message,
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

    public static ApiResponse<Void> ok(String successMessage) {

        return ok(null,successMessage);
    }

    public static ApiResponse<Void> fail(String code, String errorMessage) {
        return new ApiResponse<>(
                false,
                "해당 요청이 실패되었습니다." ,
                null,
                new ErrorBody(code, errorMessage)
        );
    }

    public record ErrorBody(String code, String message) {
    }
}
