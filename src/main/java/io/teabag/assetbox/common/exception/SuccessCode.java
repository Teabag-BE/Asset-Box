package io.teabag.assetbox.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SuccessCode {
    USER_CREATED("계정이 정상적으로 생성되었습니다."),


    // POST
    POST_CREATED("게시물이 정상적으로 생성되었습니다."),
    POST_UPDATED("게시물이 정상적으로 수정되었습니다."),

    // Category
    CATEGORY_READ("카테고리의 대분류가 정상적으로 조회되었습니다."),
    CATEGORY_CHILDREN_READ("해당 카테고리의 자식카테고리가 정상적으로 조회되었습니다.")

    ;

    private final String successMessage;
}
