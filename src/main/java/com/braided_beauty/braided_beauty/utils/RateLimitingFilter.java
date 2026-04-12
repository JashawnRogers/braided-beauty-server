package com.braided_beauty.braided_beauty.utils;

import com.braided_beauty.braided_beauty.config.RateLimitingConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@Order(1)
@ConditionalOnProperty(name = "app.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String AVAILABILITY_PREFIX = "availability";
    private static final String BOOKING_PREFIX = "booking";
    private static final String AUTH_PREFIX = "auth";

    private final RateLimitingConfig rateLimitingConfig;
    private final ProxyManager<String> proxyManager;

    public RateLimitingFilter(RateLimitingConfig rateLimitingConfig, ProxyManager<String> proxyManager) {
        this.rateLimitingConfig = rateLimitingConfig;
        this.proxyManager = proxyManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = resolveClientIp(request);

        String bucketPrefix = resolveBucketPrefix(path);
        if (bucketPrefix == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Supplier<BucketConfiguration> bucketConfiguration = resolveBucketConfiguration(bucketPrefix);
        String bucketKey = bucketPrefix + ":" + clientIp;

        Bucket bucket = proxyManager.getProxy(bucketKey, bucketConfiguration);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        writeRateLimitExceededResponse(response, probe);
    }

    private String resolveBucketPrefix(String path) {
        if (path.startsWith("/api/v1/availability")) {
            return AVAILABILITY_PREFIX;
        }

        if (path.startsWith("/api/v1/appointments/book")) {
            return BOOKING_PREFIX;
        }

        if (path.startsWith("/api/v1/auth")) {
            return AUTH_PREFIX;
        }

        return null;
    }

    private Supplier<BucketConfiguration> resolveBucketConfiguration(String bucketPrefix) {
        return switch (bucketPrefix) {
            case AVAILABILITY_PREFIX -> rateLimitingConfig.availabilityBucketConfig();
            case BOOKING_PREFIX -> rateLimitingConfig.bookingBucketConfig();
            case AUTH_PREFIX -> rateLimitingConfig.authBucketConfig();
            default -> throw new IllegalArgumentException("Unsupported bucket prefix: " + bucketPrefix);
        };
    }

    /**
     * .properties is set to server.forward-headers-strategy=framework
     *  so true IP should not get blocked by proxies
     * **/
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private void writeRateLimitExceededResponse(HttpServletResponse response, ConsumptionProbe probe) throws IOException {
        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));

        response.setStatus(429);
        response.setContentType("text/plain");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setHeader("X-RateLimit-Retry-After-Seconds", String.valueOf(retryAfterSeconds));
        response.getWriter().write("Too many requests. Please try again later.");
    }
}
