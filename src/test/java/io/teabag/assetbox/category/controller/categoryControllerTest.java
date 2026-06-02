package io.teabag.assetbox.category.controller;

import io.teabag.assetbox.category.CategoryController;
import io.teabag.assetbox.category.dto.CategoryResponse;
import io.teabag.assetbox.category.service.CategoryService;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CategoryService categoryService;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("전체 카테고리 조회 요청 시 200 OK와 전체 카테고리 목록을 반환한다")
    void findAll_success() throws Exception {
        // given
        List<CategoryResponse> responses = List.of(
                new CategoryResponse(1L, "소품", null, 1),
                new CategoryResponse(2L, "가구", 1L, 2),
                new CategoryResponse(3L, "의자", 2L, 3)
        );

        given(categoryService.findAll())
                .willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("소품"))
                .andExpect(jsonPath("$.data[0].parentId").isEmpty())
                .andExpect(jsonPath("$.data[0].depth").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("가구"))
                .andExpect(jsonPath("$.data[1].parentId").value(1))
                .andExpect(jsonPath("$.data[1].depth").value(2))
                .andExpect(jsonPath("$.data[2].id").value(3))
                .andExpect(jsonPath("$.data[2].name").value("의자"))
                .andExpect(jsonPath("$.data[2].parentId").value(2))
                .andExpect(jsonPath("$.data[2].depth").value(3));

        then(categoryService)
                .should()
                .findAll();
    }

    //실패 테스트
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("전체 카테고리 조회 실패 - 서비스 예외 발생 시 404 CATEGORY_NOT_FOUND를 반환한다")
    void findAll_fail_when_service_throws_exception() throws Exception {
        // given
        given(categoryService.findAll())
                .willThrow(new BusinessException(
                        ErrorCode.CATEGORY_NOT_FOUND,
                        "Category not found"
                ));

        // when & then
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
        then(categoryService)
                .should()
                .findAll();
    }

}