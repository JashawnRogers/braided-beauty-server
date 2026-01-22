package com.braided_beauty.braided_beauty.records;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.s3")
public record S3Properties(
        String accessKey,
        String secretKey,
        String region,
        String bucketName,
        String url
) {
}
