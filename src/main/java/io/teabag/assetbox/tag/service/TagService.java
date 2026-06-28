package io.teabag.assetbox.tag.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.tag.domain.Tag;
import io.teabag.assetbox.tag.dto.PopularTagResponse;
import io.teabag.assetbox.tag.repository.TagRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TagService {

    private static final int MAX_TAG_NAME_LENGTH = 30;
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("^[가-힣A-Za-z0-9_-]+$");

    private final TagRepository tagRepository;

    @Transactional
    public Set<Tag> findOrCreateAll(Collection<String> names) {
        Set<Tag> tags = new LinkedHashSet<>();

        for (String normalizedName : normalizeAll(names)) {
            tags.add(findOrCreate(normalizedName));
        }

        return tags;
    }

    private Set<String> normalizeAll(Collection<String> names) {
        Set<String> normalizedNames = new LinkedHashSet<>();

        if (names == null) {
            return normalizedNames;
        }

        for (String name : names) {
            String normalizedName = normalize(name);
            if (!normalizedName.isBlank()) {
                normalizedNames.add(normalizedName);
            }
        }

        return normalizedNames;
    }

    private String normalize(String name) {
        if (name == null) {
            return "";
        }

        String normalizedName = name.trim().toLowerCase(Locale.ROOT);

        if (normalizedName.isBlank()) {
            return "";
        }

        if (normalizedName.length() > MAX_TAG_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.TAG_NAME_TOO_LONG);
        }

        if (!TAG_NAME_PATTERN.matcher(normalizedName).matches()) {
            throw new BusinessException(ErrorCode.TAG_NAME_INVALID_CHAR);
        }

        return normalizedName;
    }

    private Tag findOrCreate(String normalizedName) {
        return tagRepository.findByName(normalizedName)
                .orElseGet(() -> saveNewTag(normalizedName));
    }

    private Tag saveNewTag(String normalizedName) {
        try {
            return tagRepository.save(new Tag(normalizedName));
        } catch (DataIntegrityViolationException exception) {
            return tagRepository.findByName(normalizedName)
                    .orElseThrow(() -> exception);
        }
    }

    public List<PopularTagResponse> popularTags(Integer limit) {
        int resolvedLimit = (limit == null) ? 10 : limit;

        if (resolvedLimit < 1 || resolvedLimit > 50) {
            throw new BusinessException(ErrorCode.LIMIT_TOO_LARGE);
        }

        return tagRepository.findPopularTags(PageRequest.of(0, resolvedLimit));
    }

}
