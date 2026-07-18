package io.teabag.assetbox.common.config;

import io.teabag.assetbox.common.properties.RedisProperties;
import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@RequiredArgsConstructor
public class RedisConfiguration {

    private final RedisProperties redisProperties;

    @Bean
    public RedissonClient redisClient(){
        Config config = new Config();
        String uri = String.format("redis://%s:%s", redisProperties.getHost(), redisProperties.getPort());
        config.useSingleServer().setAddress(uri);
        return Redisson.create(config);
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory(){
        return new LettuceConnectionFactory(
                redisProperties.getHost(),
                Integer.parseInt(redisProperties.getPort())
        );
    }

    @Bean
    public RedisConnectionFactory emailValidationConnectionFactory(){
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(
                redisProperties.getHost(),
                Integer.parseInt(redisProperties.getPort())
        );
        redisStandaloneConfiguration.setDatabase(2);
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }
    @Bean
    @Qualifier("email")
    public RedisTemplate<String,String> emailValidationRedisTemplate(){
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(emailValidationConnectionFactory());
        return redisTemplate;
    }
}
