package io.teabag.assetbox.post.dto;

import java.util.List;

public record PostViewerResponse(
    Long postId,
    PostViewerFileResponse model,
    List<PostViewerFileResponse> textures
) {
}
