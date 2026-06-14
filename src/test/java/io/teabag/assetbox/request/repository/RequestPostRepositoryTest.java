package io.teabag.assetbox.request.repository;

import io.teabag.assetbox.common.config.JpaConfig;
import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.repository.UserRepository;
import io.teabag.assetbox.util.RequestPostUtil;
import io.teabag.assetbox.util.UserUtil;
import org.aspectj.lang.annotation.Before;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaConfig.class)
@DisplayName("Description : RequestPostRepository의")
class RequestPostRepositoryTest {
    @Autowired
    RequestPostRepository requestPostRepository;
    @Autowired
    UserRepository userRepository;

    @Nested
    @DisplayName("Description : getCountByRequesterId() 메서드에서")
    class Description_on_getCountByRequesterId{

        User testUser;

        @BeforeEach
        void setUp(){
            testUser = userRepository.save(
                    UserUtil.createUser(
                            "testUser@naver.com",
                            "usfgauifgeiuawfgiwuafaul"
                    )
            );

            requestPostRepository.save(
                    RequestPostUtil.create(testUser.getId())
            );
            requestPostRepository.save(
                    RequestPostUtil.create(testUser.getId())
            );
        }

        @Nested
        @DisplayName("Context : 올바른 데이터가 주어진 경우")
        class Context_with_valid_data{

            @Test
            @DisplayName("It : 요청글 수 세기 성공")
            void 요청글_카운트_성공(){
                // given
                // 다른 사용자가 작성한 게시글이 존재하더라도 작성자 ID의 게시글만 조회
                requestPostRepository.save(
                        RequestPostUtil.create(131231L)
                );

                // when
                Integer count = requestPostRepository.getCountByRequesterId(testUser.getId());

                // then
                Assertions.assertThat(count).isNotNull();
                Assertions.assertThat(count).isEqualTo(2);
            }

        }

    }
}