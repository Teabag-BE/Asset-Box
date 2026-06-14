package io.teabag.assetbox.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.teabag.assetbox.category.domain.Category;
import io.teabag.assetbox.category.dto.CategoryResponse;
import io.teabag.assetbox.category.dto.CategoryTreeResponse;
import io.teabag.assetbox.category.repository.CategoryRepository;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService")
@ActiveProfiles("test")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category1;
    private Category category2;
    private Category depth1Category;
    private Category depth2Category;
    private Category depth3Category;

    @BeforeEach
    void setUp() throws Exception {
        category1 = new Category("대분류1", null, 1);
        setField(category1, "id", 1L);

        category2 = new Category("대분류2", null, 1);
        setField(category2, "id", 2L);

        depth1Category = new Category("depth1", null, 1);
        setField(depth1Category, "id", 10L);

        depth2Category = new Category("depth2", 1L, 2);
        setField(depth2Category, "id", 20L);

        depth3Category = new Category("depth3", 2L, 3);
        setField(depth3Category, "id", 30L);
    }

    @Test
    @DisplayName("findAll은 전체 카테고리를 CategoryResponse 목록으로 반환한다")
    void findAll_should_return_all_categories_as_response_list() {
        // given
        given(categoryRepository.findAll())
                .willReturn(List.of(category1, category2));

        // when
        List<CategoryResponse> result = categoryService.findAll();

        // then
        assertThat(result).hasSize(2);

        assertThat(result.get(0).id()).isEqualTo(category1.getId());
        assertThat(result.get(0).name()).isEqualTo(category1.getName());
        assertThat(result.get(0).parentId()).isEqualTo(category1.getParentId());
        assertThat(result.get(0).depth()).isEqualTo(category1.getDepth());

        assertThat(result.get(1).id()).isEqualTo(category2.getId());
        assertThat(result.get(1).name()).isEqualTo(category2.getName());
        assertThat(result.get(1).parentId()).isEqualTo(category2.getParentId());
        assertThat(result.get(1).depth()).isEqualTo(category2.getDepth());

        then(categoryRepository)
                .should()
                .findAll();
    }

    @Test
    @DisplayName("findAll은 카테고리가 없을 때 빈 리스트를 반환한다")
    void findAll_should_return_empty_list_when_no_categories() {
        // given
        given(categoryRepository.findAll())
                .willReturn(List.of());

        // when
        List<CategoryResponse> result = categoryService.findAll();

        // then
        assertThat(result).isEmpty();

        then(categoryRepository)
                .should()
                .findAll();
    }

    //실패 테스트
    @Test
    @DisplayName("findAll은 Repository 예외 발생 시 그대로 전파한다")
    void findAll_should_propagate_exception_when_repository_fails() {
        // given
        given(categoryRepository.findAll())
                .willThrow(new RuntimeException("DB error"));

        // when & then
        assertThatThrownBy(() -> categoryService.findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");

        then(categoryRepository)
                .should()
                .findAll();
    }

    @Test
    @DisplayName("findTree는 평평한 카테고리 목록을 트리 구조로 변환해 반환한다")
    void findTree_should_convert_flat_list_to_tree_structure() throws Exception {
        // given
        Category root = new Category("소품", null, 1);
        setField(root, "id", 1L);

        Category child = new Category("가구", 1L, 2);
        setField(child, "id", 2L);

        Category grandChild = new Category("의자", 2L, 3);
        setField(grandChild, "id", 3L);

        given(categoryRepository.findAll())
                .willReturn(List.of(root, child, grandChild));

        // when
        List<CategoryTreeResponse> result = categoryService.findTree();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("소품");
        assertThat(result.get(0).depth()).isEqualTo(1);
        assertThat(result.get(0).children()).hasSize(1);

        CategoryTreeResponse childNode = result.get(0).children().get(0);
        assertThat(childNode.id()).isEqualTo(2L);
        assertThat(childNode.name()).isEqualTo("가구");
        assertThat(childNode.depth()).isEqualTo(2);
        assertThat(childNode.children()).hasSize(1);

        CategoryTreeResponse grandChildNode = childNode.children().get(0);
        assertThat(grandChildNode.id()).isEqualTo(3L);
        assertThat(grandChildNode.name()).isEqualTo("의자");
        assertThat(grandChildNode.depth()).isEqualTo(3);
        assertThat(grandChildNode.children()).isEmpty();

        then(categoryRepository)
                .should()
                .findAll();
    }

    @Test
    @DisplayName("findTree는 카테고리가 없을 때 빈 리스트를 반환한다")
    void findTree_should_return_empty_list_when_no_categories() {
        // given
        given(categoryRepository.findAll())
                .willReturn(List.of());

        // when
        List<CategoryTreeResponse> result = categoryService.findTree();

        // then
        assertThat(result).isEmpty();

        then(categoryRepository)
                .should()
                .findAll();
    }

    @Test
    @DisplayName("findTree는 Repository 예외 발생 시 예외를 전파한다")
    void findTree_should_propagate_exception_when_repository_fails() {
        // given
        given(categoryRepository.findAll())
                .willThrow(new RuntimeException("DB error"));

        // when & then
        assertThatThrownBy(() -> categoryService.findTree())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");

        then(categoryRepository)
                .should()
                .findAll();
    }

    @Test
    @DisplayName("validatePostCategory는 categoryId가 null이면 VALIDATION_FAILED 예외를 던진다")
    void validatePostCategory_should_throw_validation_failed_when_categoryId_is_null() {
        // when & then
        assertThatThrownBy(() -> categoryService.validatePostCategory(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                });
    }

    @Test
    @DisplayName("validatePostCategory는 categoryId가 존재하지 않으면 CATEGORY_NOT_FOUND 예외를 던진다")
    void validatePostCategory_should_throw_category_not_found_when_categoryId_not_exist() {
        // given
        given(categoryRepository.findByIdOrThrow(999L))
                .willThrow(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Category not found"));

        // when & then
        assertThatThrownBy(() -> categoryService.validatePostCategory(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
                });

        then(categoryRepository)
                .should()
                .findByIdOrThrow(999L);
    }

    @Test
    @DisplayName("validatePostCategory는 depth=1 카테고리면 CATEGORY_DEPTH_INVALID 예외를 던진다")
    void validatePostCategory_should_throw_category_depth_invalid_when_depth_is_1() {
        // given
        given(categoryRepository.findByIdOrThrow(10L)).willReturn(depth1Category);

        // when & then
        assertThatThrownBy(() -> categoryService.validatePostCategory(10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_DEPTH_INVALID);
                });

        then(categoryRepository)
                .should()
                .findByIdOrThrow(10L);
    }

    @Test
    @DisplayName("validatePostCategory는 depth=2 카테고리면 CATEGORY_DEPTH_INVALID 예외를 던진다")
    void validatePostCategory_should_throw_category_depth_invalid_when_depth_is_2() {
        // given
        given(categoryRepository.findByIdOrThrow(20L)).willReturn(depth2Category);

        // when & then
        assertThatThrownBy(() -> categoryService.validatePostCategory(20L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_DEPTH_INVALID);
                });

        then(categoryRepository)
                .should()
                .findByIdOrThrow(20L);
    }

    @Test
    @DisplayName("validatePostCategory는 depth=3 카테고리면 Category를 정상 반환한다")
    void validatePostCategory_should_return_category_when_depth_is_3() {
        // given
        given(categoryRepository.findByIdOrThrow(30L)).willReturn(depth3Category);

        // when
        Category result = categoryService.validatePostCategory(30L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(30L);
        assertThat(result.getName()).isEqualTo("depth3");
        assertThat(result.getDepth()).isEqualTo(3);

        then(categoryRepository)
                .should()
                .findByIdOrThrow(30L);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}