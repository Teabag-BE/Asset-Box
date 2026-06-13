package io.teabag.assetbox.user.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.post.repository.PostLikeRepository;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.request.repository.RequestPostRepository;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.EmailWhiteList;
import io.teabag.assetbox.user.domain.QUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.*;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
            String role,
            String q,
            PageRequest pageRequest
    ) {
        BooleanBuilder booleanBuilder = new BooleanBuilder()
                .and(containsRole(role))
                .and(containsNameOrNickname(q));
        OrderSpecifier<Long> asc = qUser.id.asc();

        List<UserInfoResponse> results = jpaQueryFactory.select(
                        new QUserInfoResponse(
                                qUser.id,
                                qUser.name,
                                qUser.nickname
                        )
                ).from(qUser)
                .where(booleanBuilder)
                .orderBy(asc)
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();
        results.forEach((userInfo)->{
            userInfo.setPostCount(
                    postRepository.getCountByRequesterId(userInfo.getId())
            );
            userInfo.setTotalLikes(postLikeRepository.getCountByUserId(userInfo.getId()));
        });

        results = (Strings.isNotBlank(sortColumn) && Strings.isNotBlank(sortType))?
                trySort(results, sortColumn, sortType) : results;

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
        return (Strings.isNotBlank(role)) ? qUser.role.eq(Role.valueOf(role.toUpperCase())) : null;
    }
    private BooleanExpression containsUsernameOrEmail(String q){
        return (Strings.isNotBlank(q)) ? qUser.publicEmail.containsIgnoreCase(q)
                                         .or(qUser.name.containsIgnoreCase(q)) : null;
    }
    private BooleanExpression containsNameOrNickname(String q){
        return (Strings.isNotBlank(q)) ? qUser.name.containsIgnoreCase(q)
                                         .or(qUser.nickname.containsIgnoreCase(q)) : null;
    }
    private List<UserInfoResponse> trySort(List<UserInfoResponse> results, String sortColumn, String sortType){
        return switch( sortColumn ){
            case "nickname" -> {
                Stream<UserInfoResponse> sortedResult = results.stream();
                if( sortType.equals("desc") ) sortedResult = sortedResult.sorted(Comparator.comparing(UserInfoResponse::getNickname).reversed());
                else sortedResult = sortedResult.sorted(Comparator.comparing(UserInfoResponse::getNickname));

                yield sortedResult.toList();
            }
            case "totalLikes" ->{
                Stream<UserInfoResponse> sortedResult = results.stream();
                if( sortType.equals("desc") ) sortedResult = sortedResult.sorted(Comparator.comparing(UserInfoResponse::getTotalLikes).reversed());
                else sortedResult = sortedResult.sorted(Comparator.comparing(UserInfoResponse::getTotalLikes));

                yield sortedResult.toList();
            }
            default -> {
                Stream<UserInfoResponse> sortedResult = results.stream();
                if( sortType.equals("desc") ) sortedResult = sortedResult.sorted(Comparator.comparing(UserInfoResponse::getPostCount).reversed());
                else sortedResult = sortedResult.sorted(Comparator.comparing(UserInfoResponse::getPostCount));

                yield sortedResult.toList();
            }
        };

    }
}
