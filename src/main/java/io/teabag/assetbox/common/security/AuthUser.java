package io.teabag.assetbox.common.security;

import java.util.Set;

public record AuthUser(Long id, String email, Set<String> roles) {
}
