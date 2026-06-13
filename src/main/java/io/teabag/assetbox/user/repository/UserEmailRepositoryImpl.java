package io.teabag.assetbox.user.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.teabag.assetbox.post.domain.PostLike;
import io.teabag.assetbox.post.repository.PostLikeRepository;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.request.repository.RequestPostRepository;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.EmailWhiteList;
import io.teabag.assetbox.user.domain.QUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.AdminsUserDetailResponse;
import io.teabag.assetbox.user.dto.QUserDetailsResponse;
import io.teabag.assetbox.user.dto.UserDetailsResponse;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserEmailRepositoryImpl implements UserEmailRepository{
    private final UserRepository userRepository;
    private final EmailWhiteListRepository emailWhiteListRepository;
    private final PostRepository postRepository;
    private final RequestPostRepository requestPostRepository;
    private final PostLikeRepository postLikeRepository;


    private final JPAQueryFactory jpaQueryFactory;

    private final QUser qUser = QUser.user;

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

    @Override
    public AdminsUserDetailResponse findUserByAdmin(
            String role,
            String q,
            PageRequest pageRequest
    ) {
        BooleanBuilder booleanBuilder = new BooleanBuilder()
                .and(containsRole(role))
                .and(containsUsernameOrEmail(q));
        List<UserDetailsResponse> results = jpaQueryFactory.select(
                        new QUserDetailsResponse(
                                qUser.id,
                                qUser.email,
                                qUser.nickname,
                                qUser.major,
                                qUser.provider,
                                qUser.role,
                                qUser.isOauthLinked
                        )
                ).from(qUser)
                .where(booleanBuilder)
                .orderBy(qUser.id.asc())
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();
        results.forEach((userDetail)->{
            userDetail.setPostCount(
                    postRepository.getCountByRequesterId(userDetail.getId())
                    + requestPostRepository.getCountByRequesterId(userDetail.getId())
            );
            userDetail.setTotalLikes(postLikeRepository.getCountByUserId(userDetail.getId()));
        });

        int totalSize = jpaQueryFactory.selectFrom(qUser)
                .where(booleanBuilder)
                .fetch()
                .size();

        int totalPage = (int) Math.ceil((double)totalSize / pageRequest.getPageSize());

        return AdminsUserDetailResponse.builder()
                .items(results)
                .page(pageRequest.getPageNumber())
                .size(pageRequest.getPageSize())
                .totalElements(totalSize)
                .totalPages(totalPage)
                .first( (pageRequest.getPageNumber() == 0) ? true : false )
                .last( ((pageRequest.getPageNumber() == totalPage - 1) && ( pageRequest.getPageNumber() != 0 ) ) ? true : false )
                .build();
    }
    public BooleanExpression containsRole(String role){
        return (Strings.isNotBlank(role)) ? qUser.role.eq(Role.valueOf(role.toUpperCase())) : null;
    }
    public BooleanExpression containsUsernameOrEmail(String q){
        return (Strings.isNotBlank(q)) ? qUser.publicEmail.containsIgnoreCase(q)
                                         .or(qUser.name.containsIgnoreCase(q)) : null;
    }
}
