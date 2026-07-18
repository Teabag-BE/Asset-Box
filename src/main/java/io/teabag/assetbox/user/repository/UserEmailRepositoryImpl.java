package io.teabag.assetbox.user.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.email.repository.EmailWhiteListRepository;
import io.teabag.assetbox.post.domain.QPost;
import io.teabag.assetbox.post.domain.QPostLike;
import io.teabag.assetbox.post.repository.PostLikeRepository;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.request.repository.RequestPostRepository;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.email.domain.EmailWhiteList;
import io.teabag.assetbox.user.domain.QUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.*;
import io.teabag.assetbox.user.dto.directory.QUserInfoResponse;
import io.teabag.assetbox.user.dto.directory.SearchUserResponse;
import io.teabag.assetbox.user.dto.directory.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
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
    private final QPost qPost = QPost.post;
    private final QPostLike qPostLike = QPostLike.postLike;

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
    public EmailWhiteList findEmailWhiteListByEmailOrThrow(String email) {
        return emailWhiteListRepository.findByEmailOrThrow(email);
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
    public SearchUserByAdminResponse findUserByAdmin(
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
            );
            userDetail.setTotalLikes(postLikeRepository.getCountByUserId(userDetail.getId()));
        });

        int totalSize = jpaQueryFactory.selectFrom(qUser)
                .where(booleanBuilder)
                .fetch()
                .size();

        int totalPage = (int) Math.ceil((double)totalSize / pageRequest.getPageSize());

        return SearchUserByAdminResponse.builder()
                .items(results)
                .page(pageRequest.getPageNumber())
                .size(pageRequest.getPageSize())
                .totalElements(totalSize)
                .totalPages(totalPage)
                .first( (pageRequest.getPageNumber() == 0) ? true : false )
                .last( ((pageRequest.getPageNumber() == totalPage - 1) && ( pageRequest.getPageNumber() != 0 ) ) ? true : false )
                .build();
    }

    @Override
    public SearchUserResponse findUser(
            String sortColumn,
            String sortType,
            String major,
            String q,
            PageRequest pageRequest
    ) {
        BooleanBuilder booleanBuilder = new BooleanBuilder()
                .and(containsMajor(major))
                .and(containsNameOrNickname(q));
        JPAQuery<UserInfoResponse> query = jpaQueryFactory.select(
                        new QUserInfoResponse(
                                qUser.id,
                                qUser.name,
                                qUser.nickname,
                                qPost.id.count(),
                                qPostLike.id.count()
                        )
                ).from(qUser)
                .leftJoin(qPost).on(qUser.id.eq(qPost.authorId))
                .leftJoin(qPostLike).on(qPost.id.eq(qPostLike.postId))
                .where(booleanBuilder)
                .groupBy(qUser.id);
        query = switch( sortColumn ){
            case "nickname"->{
                if(sortType.equals("desc")) yield query.orderBy(qUser.nickname.desc());
                else yield query.orderBy(qUser.nickname.asc());
            }
            case "totalLikes"->{
                if(sortType.equals("desc")) yield query.orderBy(qPostLike.id.count().desc());
                else yield query.orderBy(qPostLike.id.count().asc());
            }
            default ->{
                if(sortType.equals("desc")) yield query.orderBy(qPost.id.count().desc());
                else yield query.orderBy(qPost.id.count().asc());
            }
        };
        List<UserInfoResponse> results = query
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();
        results.forEach(it ->
                log.info(
                        "{} / likes={} / posts={}",
                        it.getNickname(),
                        it.getTotalLikes(),
                        it.getPostCount()
                )
        );
        int totalSize = jpaQueryFactory.selectFrom(qUser)
                .where(booleanBuilder)
                .fetch()
                .size();
        int totalPage = (int) Math.ceil((double)totalSize / pageRequest.getPageSize());
        return SearchUserResponse.builder()
                .items(results)
                .page(pageRequest.getPageNumber())
                .size(pageRequest.getPageSize())
                .totalElements(totalSize)
                .totalPages(totalPage)
                .first( (pageRequest.getPageNumber() == 0) ? true : false )
                .last( ((pageRequest.getPageNumber() == totalPage - 1) && ( pageRequest.getPageNumber() != 0 ) ) ? true : false )
                .build();
    }
    @Override
    public User findByIdOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(
                ()-> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );
    }
    private BooleanExpression containsRole(String role){
        try{
            return qUser.role.eq(Role.valueOf(role.toUpperCase()));
        } catch (Exception e){
            log.info(e.getMessage());
            return null;
        }
    }
    private BooleanExpression containsMajor(String major){
        try {
            return qUser.major.eq(Major.valueOf(major.toUpperCase()));
        } catch (Exception e){
            return null;
        }
    }
    private BooleanExpression containsUsernameOrEmail(String q){
        return (Strings.isNotBlank(q)) ? qUser.publicEmail.containsIgnoreCase(q)
                                         .or(qUser.name.containsIgnoreCase(q)) : null;
    }
    private BooleanExpression containsNameOrNickname(String q){
        return (Strings.isNotBlank(q)) ? qUser.name.containsIgnoreCase(q)
                                         .or(qUser.nickname.containsIgnoreCase(q)) : null;
    }
}
