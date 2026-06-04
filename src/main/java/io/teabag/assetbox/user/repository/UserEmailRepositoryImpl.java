package io.teabag.assetbox.user.repository;

import io.teabag.assetbox.user.domain.EmailWhiteList;
import io.teabag.assetbox.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserEmailRepositoryImpl implements UserEmailRepository{
    private final UserRepository userRepository;
    private final EmailWhiteListRepository emailWhiteListRepository;
    @Override
    public User userSave(User user) {
        return userRepository.save(user);
    }

    @Override
    public boolean existsUserByEmail(String email) {
        return userRepository.existsUserByEmail(email);
    }

    @Override
    public User findByEmailOrThrow(String email) {
        return userRepository.findByEmailOrThrow(email);
    }

    @Override
    public boolean existsWhiteListByEmail(String email) {
        return emailWhiteListRepository.existsByEmail(email);
    }

    @Override
    public EmailWhiteList emailWhiteListSave(EmailWhiteList emailWhiteList) {
        return emailWhiteListRepository.save(emailWhiteList);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
