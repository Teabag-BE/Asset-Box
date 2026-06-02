package io.teabag.assetbox.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.teabag.assetbox.category.domain.Category;
import io.teabag.assetbox.category.dto.CategoryResponse;
import io.teabag.assetbox.category.repository.CategoryRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    CategoryService categoryService;

    @Test
    @DisplayName("findAll은 모든 카테고리를 id 오름차순으로 정렬하여 반환한다")
    void findAll_should_return_all_categories_sorted_by_id() {
        // given
        Category depth1 = new Category("소품", null, 1);
        setField(depth1, "id", 1L);

        Category depth2 = new Category("가구", 1L, 2);
        setField(depth2, "id", 2L);

        Category depth3 = new Category("의자", 2L, 3);
        setField(depth3, "id", 3L);

        given(categoryRepository.findAll())
                .willReturn(List.of(depth3, depth1, depth2));

        // when
        List<CategoryResponse> result = categoryService.findAll();

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("소품");
        assertThat(result.get(0).parentId()).isNull();
        assertThat(result.get(0).depth()).isEqualTo(1);
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(2).id()).isEqualTo(3L);

        then(categoryRepository)
                .should()
                .findAll();
    }

    @Test
    @DisplayName("findAll은 저장된 카테고리가 없으면 빈 리스트를 반환한다")
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
    @DisplayName("findAll은 Repository 조회 중 예외가 발생하면 예외를 전파한다")
    void findAll_should_throw_exception_when_repository_fails() {
        // given
        given(categoryRepository.findAll())
                .willThrow(new RuntimeException("Category query failed"));

        // when & then
        assertThatThrownBy(() -> categoryService.findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Category query failed");

        then(categoryRepository)
                .should()
                .findAll();
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}