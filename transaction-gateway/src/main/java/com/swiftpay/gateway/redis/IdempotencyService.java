package com.swiftpay.gateway.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {

    private static final Duration IDEMPOTENCY_TTL =
            Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public boolean exists(String transactionId) {

        String key = buildKey(transactionId);

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(key)
        );
    }

    public void save(String transactionId) {

        String key = buildKey(transactionId);

        redisTemplate.opsForValue().set(
                key,
                "PROCESSED",
                IDEMPOTENCY_TTL
        );
    }

    private String buildKey(String transactionId) {

        return "swiftpay:idempotency:" + transactionId;
    }
}
