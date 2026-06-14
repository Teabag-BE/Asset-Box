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
import io.teabag.assetbox.tag.dto.PopularTagResponse;
import io.teabag.assetbox.tag.repository.TagRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    @Test
    @DisplayName("popularTags: limit이 null이면 기본값 10으로 조회한다")
    void popularTags_usesDefaultLimitWhenNull() {
        // given
        given(tagRepository.findPopularTags(any(Pageable.class)))
                .willReturn(List.of());

        // when
        tagService.popularTags(null);

        // then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        then(tagRepository).should().findPopularTags(captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getPageNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("popularTags: limit이 5이면 PageRequest(0, 5)로 조회하고 결과를 반환한다")
    void popularTags_returnsResultsForGivenLimit() {
        // given
        PopularTagResponse tag1 = new PopularTagResponse("spring", 15L);
        PopularTagResponse tag2 = new PopularTagResponse("jpa", 10L);

        given(tagRepository.findPopularTags(PageRequest.of(0, 5)))
                .willReturn(List.of(tag1, tag2));

        // when
        List<PopularTagResponse> result = tagService.popularTags(5);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("spring");
        assertThat(result.get(0).count()).isEqualTo(15L);
        assertThat(result.get(1).name()).isEqualTo("jpa");
        assertThat(result.get(1).count()).isEqualTo(10L);

        then(tagRepository).should().findPopularTags(PageRequest.of(0, 5));
    }

    @Test
    @DisplayName("popularTags: limit이 0이면 LIMIT_TOO_LARGE 예외를 던진다")
    void popularTags_throwsWhenLimitIsZero() {
        assertThatThrownBy(() -> tagService.popularTags(0))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LIMIT_TOO_LARGE)
                );

        then(tagRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("popularTags: limit이 50을 초과하면 LIMIT_TOO_LARGE 예외를 던진다")
    void popularTags_throwsWhenLimitIsGreaterThanMax() {
        assertThatThrownBy(() -> tagService.popularTags(51))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LIMIT_TOO_LARGE)
                );

        then(tagRepository).shouldHaveNoInteractions();
    }

}
