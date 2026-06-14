package io.teabag.assetbox.tag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.tag.domain.Tag;
import io.teabag.assetbox.tag.repository.TagRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    TagRepository tagRepository;

    @InjectMocks
    TagService tagService;

    @Test
    @DisplayName("태그 이름을 정규화하고 공백과 중복을 제거한 뒤 조회하거나 생성한다")
    void findOrCreateAll_normalizesAndDeduplicates() {
        // given
        Tag springTag = new Tag("spring");

        given(tagRepository.findByName("spring"))
                .willReturn(Optional.of(springTag));
        given(tagRepository.findByName("jpa"))
                .willReturn(Optional.empty());
        given(tagRepository.findByName("한글태그"))
                .willReturn(Optional.empty());
        given(tagRepository.save(any(Tag.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Set<Tag> result = tagService.findOrCreateAll(
                Arrays.asList(" Spring ", "spring", "", " JPA ", null, "한글태그")
        );

        // then
        assertThat(result)
                .extracting(Tag::getName)
                .containsExactly("spring", "jpa", "한글태그");

        then(tagRepository).should().findByName("spring");
        then(tagRepository).should().findByName("jpa");
        then(tagRepository).should().findByName("한글태그");
        then(tagRepository).should(times(2)).save(any(Tag.class));
    }

    @Test
    @DisplayName("태그 목록이 null이면 빈 Set을 반환한다")
    void findOrCreateAll_returnsEmptySetWhenNamesAreNull() {
        // when
        Set<Tag> result = tagService.findOrCreateAll(null);

        // then
        assertThat(result).isEmpty();
        then(tagRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("태그 이름이 30자를 초과하면 예외를 발생시킨다")
    void findOrCreateAll_throwsWhenNameIsTooLong() {
        // when & then
        assertThatThrownBy(() -> tagService.findOrCreateAll(List.of("a".repeat(31))))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TAG_NAME_TOO_LONG)
                );

        then(tagRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("태그 이름에 허용되지 않는 문자가 있으면 예외를 발생시킨다")
    void findOrCreateAll_throwsWhenNameHasInvalidChar() {
        // when & then
        assertThatThrownBy(() -> tagService.findOrCreateAll(List.of("bad!tag")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TAG_NAME_INVALID_CHAR)
                );

        then(tagRepository).shouldHaveNoInteractions();
    }

}
