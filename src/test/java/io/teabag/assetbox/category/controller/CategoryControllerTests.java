package io.teabag.assetbox.category.controller;

import io.teabag.assetbox.category.dto.CategoryResponse;
import io.teabag.assetbox.category.dto.CategoryTreeResponse;
import io.teabag.assetbox.category.service.CategoryService;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.constants.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.teabag.assetbox.user.service.TokenProvider;
import io.teabag.assetbox.user.service.UserService;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CategoryService categoryService;

    @MockitoBean
    UserService userService;

    @MockitoBean
    TokenProvider tokenProvider;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/categories는 200 OK와 전체 카테고리 목록을 반환한다")
    void findAll_success() throws Exception {
        // given
        List<CategoryResponse> responses = List.of(
                new CategoryResponse(1L, "대분류1", null, 1),
                new CategoryResponse(2L, "대분류2", null, 1)
        );

        given(categoryService.findAll())
                .willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("대분류1"))
                .andExpect(jsonPath("$.data[0].parentId").isEmpty())
                .andExpect(jsonPath("$.data[0].depth").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("대분류2"))
                .andExpect(jsonPath("$.data[1].parentId").isEmpty())
                .andExpect(jsonPath("$.data[1].depth").value(1));

        then(categoryService)
                .should()
                .findAll();
    }

    //실패 테스트
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/categories는 서비스 예외 발생 시 에러 응답을 반환한다")
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
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));

        then(categoryService)
                .should()
                .findAll();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/categories/tree는 200 OK와 트리 구조의 카테고리 목록을 반환한다")
    void findTree_success() throws Exception {
        // given
        CategoryTreeResponse grandChild = new CategoryTreeResponse(
                3L, "의자", 3, List.of()
        );
        CategoryTreeResponse child = new CategoryTreeResponse(
                2L, "가구", 2, List.of(grandChild)
        );
        CategoryTreeResponse root = new CategoryTreeResponse(
                1L, "소품", 1, List.of(child)
        );
        List<CategoryTreeResponse> tree = List.of(root);

        given(categoryService.findTree())
                .willReturn(tree);

        // when & then
        mockMvc.perform(get("/api/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("소품"))
                .andExpect(jsonPath("$.data[0].depth").value(1))
                .andExpect(jsonPath("$.data[0].children.length()").value(1))
                .andExpect(jsonPath("$.data[0].children[0].id").value(2))
                .andExpect(jsonPath("$.data[0].children[0].name").value("가구"))
                .andExpect(jsonPath("$.data[0].children[0].depth").value(2))
                .andExpect(jsonPath("$.data[0].children[0].children.length()").value(1))
                .andExpect(jsonPath("$.data[0].children[0].children[0].id").value(3))
                .andExpect(jsonPath("$.data[0].children[0].children[0].name").value("의자"))
                .andExpect(jsonPath("$.data[0].children[0].children[0].depth").value(3));

        then(categoryService)
                .should()
                .findTree();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/categories/tree는 서비스 예외 발생 시 에러 응답을 반환한다")
    void findTree_fail_when_service_throws_exception() throws Exception {
        // given
        given(categoryService.findTree())
                .willThrow(new BusinessException(
                        ErrorCode.CATEGORY_NOT_FOUND,
                        "Category not found"
                ));

        // when & then
        mockMvc.perform(get("/api/categories/tree"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));

        then(categoryService)
                .should()
                .findTree();
    }
}


