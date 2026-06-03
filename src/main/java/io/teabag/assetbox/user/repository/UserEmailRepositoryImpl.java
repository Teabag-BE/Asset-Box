package io.teabag.assetbox.user.repository;

import io.teabag.assetbox.user.domain.EmailWhiteList;
import io.teabag.assetbox.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserEmailRepositoryImpl implements UserEmailRepository{
    private final UserReposiotry userReposiotry;
    private final EmailWhiteListRepository emailWhiteListRepository;
    @Override
    public User userSave(User user) {
        return userReposiotry.save(user);
    }

    @Override
    public boolean existsUserByEmail(String email) {
        return userReposiotry.existsUserByEmail(email);
    }

    @Override
    public User findByEmailOrThrow(String email) {
        return userReposiotry.findByEmailOrThrow(email);
    }

    @Override
    public boolean existsWhiteListByEmail(String email) {
        return emailWhiteListRepository.existsByEmail(email);
    }

    @Override
    public EmailWhiteList emailWhiteListSave(EmailWhiteList emailWhiteList) {
        return emailWhiteListRepository.save(emailWhiteList);
    }
}
