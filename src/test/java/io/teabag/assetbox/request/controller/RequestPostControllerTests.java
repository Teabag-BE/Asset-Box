package io.teabag.assetbox.request.controller;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.filter.JwtFilter;
import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.dto.RequestCreateRequest;
import io.teabag.assetbox.request.dto.RequestListResponse;
import io.teabag.assetbox.request.dto.RequestResponse;
import io.teabag.assetbox.request.service.RequestPostService;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.util.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

@WebMvcTest(RequestPostController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class RequestPostControllerTests {

    @Autowired
    MockMvc mockMvc;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @MockitoBean
    RequestPostService requestPostService;


    @MockitoBean
    JwtFilter jwtFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private MockMultipartFile requestPart(RequestCreateRequest request) throws Exception {
        return new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );
    }

    private MockMultipartFile thumbnailPart() {
        return new MockMultipartFile(
                "thumbnail",
                "thumbnail.png",
                MediaType.IMAGE_PNG_VALUE,
                "thumbnail".getBytes()
        );
    }

    private MockMultipartFile referencePart(String originalName) {
        return new MockMultipartFile(
                "references",
                originalName,
                MediaType.IMAGE_PNG_VALUE,
                originalName.getBytes()
        );
    }

    private UsernamePasswordAuthenticationToken currentUserAuthentication() {
        CurrentUser currentUser = CurrentUser.builder()
                .id(1L)
                .email("user@test.com")
                .name("user")
                .role(Role.USER)
                .major(Major.TA)
                .build();
        return new UsernamePasswordAuthenticationToken(
                currentUser,
                null,
                currentUser.getAuthorities()
        );
    }




    @Nested
    @DisplayName("요청글 생성")
    class request_생성관련_테스트 {

        RequestCreateRequest request;

        @BeforeEach
        void setUp() {
            request = TestUtil.requestCreateRequestOf();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("201 Created와 성공 응답을 반환한다")
        void createRequest() throws Exception {
            // given
            RequestPost savedRequestPost = RequestPost.builder()
                    .title("요청 제목")
                    .content("요청 내용")
                    .assetType("CHARACTER")
                    .preferredStyle("LOW_POLY")
                    .engine("UNITY")
                    .deadline(LocalDateTime.now().plusDays(7))
                    .requesterId(1L)
                    .build();

            given(requestPostService.save(any(),any(RequestCreateRequest.class), any(), any()))
                    .willReturn(RequestResponse.from(savedRequestPost));

            // when
            SecurityContextHolder.getContext().setAuthentication(currentUserAuthentication());
            mockMvc.perform(
                            multipart("/api/requests")
                                    .file(requestPart(request))
                                    .file(thumbnailPart())
                                    .file(referencePart("reference-1.png"))
                                    .file(referencePart("reference-2.png"))
                                    .with(csrf())
                    )
            //then
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("요청 제목"))
                    .andExpect(jsonPath("$.data.content").value("요청 내용"))
                    .andExpect(jsonPath("$.data.assetType").value("CHARACTER"))
                    .andExpect(jsonPath("$.data.preferredStyle").value("LOW_POLY"))
                    .andExpect(jsonPath("$.data.engine").value("UNITY"))
                    .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                    .andExpect(jsonPath("$.data.requesterId").value(1L));

            ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
            ArgumentCaptor<RequestCreateRequest> requestCaptor = ArgumentCaptor.forClass(RequestCreateRequest.class);
            ArgumentCaptor<MultipartFile> thumbnailCaptor = ArgumentCaptor.forClass(MultipartFile.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<MultipartFile>> referencesCaptor = ArgumentCaptor.forClass(List.class);

            then(requestPostService)
                    .should()
                    .save(
                            currentUserCaptor.capture(),
                            requestCaptor.capture(),
                            thumbnailCaptor.capture(),
                            referencesCaptor.capture()
                    );

            assertThat(currentUserCaptor.getValue().getId()).isEqualTo(1L);
            assertThat(requestCaptor.getValue().title()).isEqualTo("요청 제목");
            assertThat(thumbnailCaptor.getValue().getOriginalFilename())
                    .isEqualTo("thumbnail.png");
            assertThat(referencesCaptor.getValue())
                    .extracting(MultipartFile::getOriginalFilename)
                    .containsExactly("reference-1.png", "reference-2.png");
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("실패 - title이 비어 있으면 400 VALIDATION_FAILED를 반환한다")
        void createRequest_fail_when_title_is_blank() throws Exception {
            // given
            RequestCreateRequest invalidRequest = new RequestCreateRequest(
                    "",
                    "요청 내용",
                    "CHARACTER",
                    "LOW_POLY",
                    "UNITY",
                    LocalDateTime.now().plusDays(7),
                    1L
            );

            // when
            mockMvc.perform(
                            multipart("/api/requests")
                                    .file(requestPart(invalidRequest))
                                    .with(csrf())
                    )
            //then
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

            then(requestPostService)
                    .shouldHaveNoInteractions();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("실패 - content가 비어 있으면 400 VALIDATION_FAILED를 반환한다")
        void createRequest_fail_when_content_is_blank() throws Exception {
            // given
            RequestCreateRequest invalidRequest = new RequestCreateRequest(
                    "요청 제목",
                    "",
                    "CHARACTER",
                    "LOW_POLY",
                    "UNITY",
                    LocalDateTime.now().plusDays(7),
                    1L
            );

            // when
            mockMvc.perform(
                            multipart("/api/requests")
                                    .file(requestPart(invalidRequest))
                                    .with(csrf())
                    )
            //then
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

            then(requestPostService)
                    .shouldHaveNoInteractions();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("실패 - deadline이 과거이면 400 VALIDATION_FAILED 반환한다")
        void createRequest_fail_when_deadline_is_past() throws Exception {
            // given
            RequestCreateRequest invalidRequest = new RequestCreateRequest(
                    "요청 제목",
                    "요청 내용",
                    "CHARACTER",
                    "LOW_POLY",
                    "UNITY",
                    LocalDateTime.now().minusDays(1),
                    1L
            );

            // when
            mockMvc.perform(
                            multipart("/api/requests")
                                    .file(requestPart(invalidRequest))
                                    .with(csrf())
                    )
            //then
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

            then(requestPostService)
                    .shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("요청글 삭제")
    class request_삭제관련_테스트 {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("요청글 삭제 요청 시 200 OK와 성공 응답을 반환한다")
        void deleteRequestPost_success() throws Exception {
            Long requestId = 1L;

            willDoNothing()
                    .given(requestPostService)
                    .deleteRequestPost(requestId);

            mockMvc.perform(
                            delete("/api/requests/{requestId}", requestId)
                                    .with(csrf())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.error").isEmpty());

            then(requestPostService)
                    .should()
                    .deleteRequestPost(requestId);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("REQUESTED 상태가 아니면 409 REQUEST_NOT_DELETABLE을 반환한다")
        void deleteRequestPost_fail_when_status_is_not_requested() throws Exception {
            Long requestId = 1L;

            willThrow(new BusinessException(ErrorCode.REQUEST_NOT_DELETABLE))
                    .given(requestPostService)
                    .deleteRequestPost(requestId);

            mockMvc.perform(
                            delete("/api/requests/{requestId}", requestId)
                                    .with(csrf())
                    )
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.error.code").value("REQUEST_NOT_DELETABLE"));

            then(requestPostService)
                    .should()
                    .deleteRequestPost(requestId);
        }
    }

    @Nested
    @DisplayName("요청글 수락")
    class request_수락관련_테스트 {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("요청글 수락 요청 시 200 OK와 성공 응답을 반환한다")
        void assignRequestPost_success() throws Exception {
            // given
            Long requestId = 1L;
            RequestPost assignedRequestPost = RequestPost.builder()
                    .title("요청 제목")
                    .content("요청 내용")
                    .assetType("CHARACTER")
                    .preferredStyle("LOW_POLY")
                    .engine("UNITY")
                    .deadline(LocalDateTime.now().plusDays(7))
                    .requesterId(1L)
                    .build();
            assignedRequestPost.assign(1L);

            given(requestPostService.assign(eq(requestId), any(CurrentUser.class)))
                    .willReturn(RequestResponse.from(assignedRequestPost));

            // when
            SecurityContextHolder.getContext().setAuthentication(currentUserAuthentication());
            mockMvc.perform(
                            patch("/api/requests/{requestId}/assign", requestId)
                                    .with(csrf())
                    )
                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.data.assigneeId").value(1L));

            then(requestPostService)
                    .should()
                    .assign(eq(requestId), any(CurrentUser.class));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("이미 다른 담당자가 수락한 요청글이면 409 REQUEST_ASSIGN_TAKEN을 반환한다")
        void assignRequestPost_fail_when_taken() throws Exception {
            // given
            Long requestId = 1L;

            given(requestPostService.assign(eq(requestId), any(CurrentUser.class)))
                    .willThrow(new BusinessException(ErrorCode.REQUEST_ASSIGN_TAKEN));

            // when
            SecurityContextHolder.getContext().setAuthentication(currentUserAuthentication());
            mockMvc.perform(
                            patch("/api/requests/{requestId}/assign", requestId)
                                    .with(csrf())
                    )
                    // then
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("REQUEST_ASSIGN_TAKEN"));

            then(requestPostService)
                    .should()
                    .assign(eq(requestId), any(CurrentUser.class));
        }
    }

    @Nested
    @DisplayName("요청게시물 조회")
    class request_조회관련_테스트 {
        @Nested
        @DisplayName("요청게시글 다건 조회")
        class GetRequestPosts{

            @Test
            @WithMockUser(roles = "USER")
            @DisplayName("요청글 목록을 조회할 수 있다")
            void getRequestPosts_success() throws Exception {
                // given
                Pageable pageable = PageRequest.of(
                        0,
                        2,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                );

                List<RequestResponse> requestPosts = List.of(
                        RequestResponse.from(RequestPost.builder()
                                .title("요청 제목1")
                                .content("요청 내용1")
                                .assetType("CHARACTER")
                                .preferredStyle("LOW_POLY")
                                .engine("UNITY")
                                .deadline(LocalDateTime.now().plusDays(7))
                                .requesterId(1L)
                                .build()),
                        RequestResponse.from(RequestPost.builder()
                                .title("요청 제목2")
                                .content("요청 내용2")
                                .assetType("PROP")
                                .preferredStyle("REALISTIC")
                                .engine("UNREAL")
                                .deadline(LocalDateTime.now().plusDays(10))
                                .requesterId(2L)
                                .build())
                );

                Slice<RequestResponse> slice = new SliceImpl<>(
                        requestPosts,
                        pageable,
                        true
                );

                given(requestPostService.getRequests(any(Pageable.class)))
                        .willReturn(RequestListResponse.fromResponses(slice));

                // when
                mockMvc.perform(
                                get("/api/requests")
                                        .param("page", "0")
                                        .param("size", "2")
                                        .param("sort", "createdAt,desc")
                        )
                        // then
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.items.length()").value(2))
                        .andExpect(jsonPath("$.data.items[0].title").value("요청 제목1"))
                        .andExpect(jsonPath("$.data.items[1].title").value("요청 제목2"))
                        .andExpect(jsonPath("$.data.page").value(0))
                        .andExpect(jsonPath("$.data.size").value(2))
                        .andExpect(jsonPath("$.data.hasNext").value(true));

                then(requestPostService)
                        .should()
                        .getRequests(any(Pageable.class));
            }


        }

        @Nested
        @DisplayName("요청게시글 단건 조회")
        class GetRequestPost {

            @Test
            @WithMockUser(roles = "USER")
            @DisplayName("요청글 단건 조회 성공")
            void getRequestPost_success() throws Exception {
                // given
                Long requestId = 1L;

                RequestPost requestPost = RequestPost.builder()
                        .title("요청 제목")
                        .content("요청 내용")
                        .assetType("CHARACTER")
                        .preferredStyle("LOW_POLY")
                        .engine("UNITY")
                        .deadline(LocalDateTime.now().plusDays(7))
                        .requesterId(1L)
                        .build();

                given(requestPostService.getRequest(requestId))
                        .willReturn(RequestResponse.from(requestPost));

                // when
                mockMvc.perform(
                                get("/api/requests/{requestId}", requestId)
                        )
                        // then
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.title").value("요청 제목"))
                        .andExpect(jsonPath("$.data.content").value("요청 내용"))
                        .andExpect(jsonPath("$.data.assetType").value("CHARACTER"))
                        .andExpect(jsonPath("$.data.status").value("REQUESTED"));

                then(requestPostService)
                        .should()
                        .getRequest(requestId);
            }

            @Test
            @WithMockUser(roles = "USER")
            @DisplayName("요청글이 없으면 404 REQUEST_NOT_FOUND")
            void getRequestPost_fail_when_not_found() throws Exception {
                // given
                Long requestId = 999L;

                given(requestPostService.getRequest(requestId))
                        .willThrow(new BusinessException(ErrorCode.REQUEST_NOT_FOUND));

                // when
                mockMvc.perform(
                                get("/api/requests/{requestId}", requestId)
                        )
                        // then
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.error.code").value("REQUEST_NOT_FOUND"));

                then(requestPostService)
                        .should()
                        .getRequest(requestId);
            }
        }
    }

}
