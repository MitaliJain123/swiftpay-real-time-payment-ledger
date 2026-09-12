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

    /**
     * Atomically reserves the transaction id (Redis SET NX).
     *
     * @return true if this is the first time the transaction id is
     *         seen, false if it was already reserved by a previous
     *         (or concurrent) request.
     */
    public boolean reserve(String transactionId) {

        String key = buildKey(transactionId);

        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(
                        key,
                        "PROCESSED",
                        IDEMPOTENCY_TTL
                )
        );
    }

    /**
     * Releases a reserved transaction id so the client can retry
     * after a processing failure.
     */
    public void release(String transactionId) {

        redisTemplate.delete(buildKey(transactionId));
    }

    private String buildKey(String transactionId) {

        return "swiftpay:idempotency:" + transactionId;
    }
}
