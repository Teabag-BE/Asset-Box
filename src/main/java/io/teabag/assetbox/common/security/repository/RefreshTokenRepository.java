package io.teabag.assetbox.common.security.repository;


import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.security.domain.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    default RefreshToken findByIdOrThrow(String refreshToken){
        return findById(refreshToken).orElseThrow(
                ()-> new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED)
        );
    }
}
