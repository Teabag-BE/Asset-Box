package io.teabag.assetbox.email.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.security.service.TokenProvider;
import io.teabag.assetbox.common.util.PreConditions;
import io.teabag.assetbox.email.domain.EmailWhiteList;
import io.teabag.assetbox.email.dto.EmailWhiteListSearch;
import io.teabag.assetbox.email.dto.EnrollEmailRequest;
import io.teabag.assetbox.email.dto.EnrollEmailResponse;
import io.teabag.assetbox.email.dto.SendMessageDto;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.dto.TokenBody;
import io.teabag.assetbox.user.repository.UserEmailRepository;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@Transactional(readOnly = true)
public class EmailService {

    @Value("${mail.auth-code-expiration-millis}")
    private int jwtEmailExpirationMs;
    @Value("${mail.baseUrl}")
    private String BASE_URL;

    private static final String EMAIL_KEY_PREFIX = "email-validation";
    private static final String TOPIC_NAME = "email-validation";

    private final UserEmailRepository userEmailRepository;

    private final TokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmailService(
            UserEmailRepository userEmailRepository,
            TokenProvider tokenProvider,
            @Qualifier("email") RedisTemplate<String, String> redisTemplate,
            KafkaTemplate<String, Object> kafkaTemplate
    ){
        this.userEmailRepository = userEmailRepository;
        this.tokenProvider = tokenProvider;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public void checkAvailableEmail(
            String email
    ){
        PreConditions.validate(
                userEmailRepository.existsWhiteListByEmail(email),
                ErrorCode.EMAIL_NOT_ON_WHITELIST
        );

        PreConditions.validate(
                !userEmailRepository.existsUserByEmail(email),
                ErrorCode.USER_EMAIL_DUPLICATED
        );

        // 토큰 발급
        String token = tokenProvider.issueValidationToken(
                email
        );

        String tokenKey = EMAIL_KEY_PREFIX + ":" + token;


        // Redis에 저장
        redisTemplate.opsForValue().set(
                tokenKey,
                token,
                jwtEmailExpirationMs,
                TimeUnit.MILLISECONDS
        );

        // Kafka Topic으로 발행
        try{
            SendMessageDto dto = new SendMessageDto(
                    email,
                    BASE_URL,
                    token
            );

            String json = objectMapper.writeValueAsString(dto);

            kafkaTemplate.send(
                    TOPIC_NAME,
                    json
            );
        } catch(JsonProcessingException e){
            throw new BusinessException(ErrorCode.ISSUE_MESSAGE_FAILED);
        }
    }

    @Transactional
    public void verifyToken(String token){
        tokenProvider.validate(token);

        String tokenKey = EMAIL_KEY_PREFIX + ":" + token;

        String serialized = redisTemplate.opsForValue().get(tokenKey);

        PreConditions.validate(
                Strings.isNotBlank(serialized),
                ErrorCode.TOKEN_NOT_VALID_FROM_EMAIL_VERIFICATION
        );

        TokenBody tokenBody = tokenProvider.parseJwt(token);

        EmailWhiteList founded = userEmailRepository.findEmailWhiteListByEmailOrThrow(tokenBody.email());

        founded.switchVerified();
    }

    @Transactional
    public EnrollEmailResponse enrollEmail(
            String userEmail,
            EnrollEmailRequest request
    ){
        User founded = userEmailRepository.findByEmailOrThrow(userEmail);

        PreConditions.validate(
                !founded.getRole().equals(Role.USER),
                ErrorCode.ACCOUNT_NOT_ADMIN
        );

        PreConditions.validate(
                !userEmailRepository.existsWhiteListByEmail(request.email()),
                ErrorCode.EMAIL_ALREADY_ON_WHITELIST
        );

        return EnrollEmailResponse.from(
                userEmailRepository.emailWhiteListSave(
                        new EmailWhiteList(
                                request.name(),
                                Major.valueOf(request.major()),
                                request.email()
                        )
                )
        );
    }

    public Page<EmailWhiteListSearch> getSearches(
        String email,
        PageRequest pageRequest
    ){
        User foundedUser = userEmailRepository.findByEmailOrThrow(email);

        PreConditions.validate(
                !foundedUser.getRole().equals(Role.USER),
                ErrorCode.ACCOUNT_NOT_ADMIN
        );

        return userEmailRepository.findEmailWhiteList(pageRequest);
    }

    public void deleteEmailFromWhiteList(
            String userEmail,
            String targetEmail
    ){
        User foundedUser = userEmailRepository.findByEmailOrThrow(userEmail);

        PreConditions.validate(
                !foundedUser.getRole().equals(Role.USER),
                ErrorCode.ACCOUNT_NOT_ADMIN
        );

        userEmailRepository.deleteEmailWhiteList(targetEmail);
    }
}
