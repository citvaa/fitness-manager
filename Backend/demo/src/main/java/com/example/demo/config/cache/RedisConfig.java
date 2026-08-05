package com.example.demo.config.cache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;

@RequiredArgsConstructor
@Configuration
public class RedisConfig {

    /** Cache name for the manager AI-insights endpoint - see AGENTS.md ("Upgrade: service layer decisions"). */
    public static final String MANAGER_INSIGHTS_CACHE = "MANAGER_INSIGHTS_CACHE";

    /** Cache name for the per-client AI progress-narrative endpoint - see AGENTS.md ("Upgrade: service layer decisions"). */
    public static final String CLIENT_PROGRESS_INSIGHT_CACHE = "CLIENT_PROGRESS_INSIGHT_CACHE";

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return baseConfig().entryTtl(Duration.ofMinutes(10));
    }

    /**
     * Per-cache-name TTL overrides layered on top of the 10-minute global default above.
     * <p>
     * {@code MANAGER_INSIGHTS_CACHE} gets a longer, 30-minute TTL: it aggregates historical data
     * (room check-in history, payments) that changes slowly, and every call is a paid Claude API
     * request - a longer TTL directly cuts cost with negligible staleness cost to the manager
     * dashboard. {@code CLIENT_PROGRESS_INSIGHT_CACHE} keeps the 10-minute global default; it is
     * explicitly evicted whenever a new {@code ClientProgressEntry} is recorded for that client
     * (see {@code ClientProgressEntryServiceImpl}), so the TTL only matters as a fallback and
     * doesn't need to be longer than the default.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
                .withCacheConfiguration(MANAGER_INSIGHTS_CACHE, baseConfig().entryTtl(Duration.ofMinutes(30)));
    }

    private RedisCacheConfiguration baseConfig() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);

        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));
    }

}
