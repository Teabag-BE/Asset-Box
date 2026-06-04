package io.teabag.assetbox.request.service;

import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.dto.RequestCreateRequest;
import io.teabag.assetbox.request.repository.RequestPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RequestPostService {

    private final RequestPostRepository requestPostRepository;

    // RequestPostService
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

}
