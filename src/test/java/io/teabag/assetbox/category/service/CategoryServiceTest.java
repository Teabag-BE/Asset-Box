package io.teabag.assetbox.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.teabag.assetbox.category.domain.Category;
import io.teabag.assetbox.category.dto.CategoryResponse;
import io.teabag.assetbox.category.repository.CategoryRepository;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category1;
    private Category category2;

    @BeforeEach
    void setUp() throws Exception {
        category1 = new Category("대분류1", null, 1);
        setField(category1, "id", 1L);

        category2 = new Category("대분류2", null, 1);
        setField(category2, "id", 2L);
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

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}