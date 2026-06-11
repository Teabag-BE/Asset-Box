package io.teabag.assetbox.request.controller;

import io.teabag.assetbox.common.filter.JwtFilter;
import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.dto.RequestCreateRequest;
import io.teabag.assetbox.request.service.RequestPostService;
import io.teabag.assetbox.util.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

            given(requestPostService.save(any(RequestCreateRequest.class)))
                    .willReturn(savedRequestPost);

            // when & then
            mockMvc.perform(
                            post("/api/requests/create")
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("요청 제목"))
                    .andExpect(jsonPath("$.data.content").value("요청 내용"))
                    .andExpect(jsonPath("$.data.assetType").value("CHARACTER"))
                    .andExpect(jsonPath("$.data.preferredStyle").value("LOW_POLY"))
                    .andExpect(jsonPath("$.data.engine").value("UNITY"))
                    .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                    .andExpect(jsonPath("$.data.requesterId").value(1L));

            then(requestPostService)
                    .should()
                    .save(any(RequestCreateRequest.class));
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

            // when & then
            mockMvc.perform(
                            post("/api/requests/create")
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
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

            // when & then
            mockMvc.perform(
                            post("/api/requests/create")
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
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

            // when & then
            mockMvc.perform(
                            post("/api/requests/create")
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

            then(requestPostService)
                    .shouldHaveNoInteractions();
        }
    }
}