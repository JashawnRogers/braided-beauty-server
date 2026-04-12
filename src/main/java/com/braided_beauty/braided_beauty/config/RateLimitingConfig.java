package com.braided_beauty.braided_beauty.config;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Supplier;

@Configuration
@ConditionalOnProperty(name = "app.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitingConfig {

    private final String REDIS_URL;

    public RateLimitingConfig(@Value("${spring.data.redis.url}") String REDIS_URL) {
        this.REDIS_URL = REDIS_URL;
    }

    @Bean
    public RedisClient redisClient() {
        return RedisClient.create(REDIS_URL);
    }
    
    @Bean
    public StatefulRedisConnection<String, byte[]> redisConnection(RedisClient redisClient) {
       return redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    public ProxyManager<String> lettuceBasedProxyManager(StatefulRedisConnection<String, byte[]> connection) {
        return Bucket4jLettuce.casBasedBuilder(connection)
                .expirationAfterWrite(
                        ExpirationAfterWriteStrategy
                                .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10))
                )
                .build();
    }

    @Bean
    public Supplier<BucketConfiguration> availabilityBucketConfig() {
        return () -> BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofSeconds(10)))
                .addLimit(limit -> limit.capacity(25).refillGreedy(25, Duration.ofMinutes(1)))
                .build();
    }

    @Bean
    public Supplier<BucketConfiguration> bookingBucketConfig() {
        return () -> BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(2).refillGreedy(2, Duration.ofMinutes(1)))
                .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofMinutes(10)))
                .build();
    }

    @Bean
    public Supplier<BucketConfiguration> authBucketConfig() {
        return () -> BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(3).refillGreedy(3, Duration.ofMinutes(1)))
                .addLimit(limit -> limit.capacity(8).refillGreedy(8,Duration.ofMinutes(10)))
                .build();
    }
}
