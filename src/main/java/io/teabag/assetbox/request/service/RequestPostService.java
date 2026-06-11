package io.teabag.assetbox.request.service;

import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.dto.RequestCreateRequest;
import io.teabag.assetbox.request.repository.RequestPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RequestPostService {

    private final RequestPostRepository requestPostRepository;

    @Transactional
    public RequestPost save(RequestCreateRequest request) {
        RequestPost requestPost = RequestPost.builder()
                .title(request.title())
                .content(request.content())
                .assetType(request.assetType())
                .preferredStyle(request.preferredStyle())
                .engine(request.engine())
                .deadline(request.deadline())
                .requesterId(request.requesterId())
                .build();

        return requestPostRepository.save(requestPost);
    }

    // 요청글 다건 조회
    @Transactional(readOnly = true)
    public Slice<RequestPost> getRequests(Pageable pageable) {
        return requestPostRepository.findAllByDeletedAtIsNull(pageable);
    }

    // 요청글 단건 조회
    @Transactional(readOnly = true)
    public RequestPost getRequest(Long requestId) {
        return requestPostRepository.findByIdOrThrow(requestId);
    }
}
