package com.braided_beauty.braided_beauty.dtos.presign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
@Getter
@Builder
public class PresignPutResponseDTO {
    private final String s3Url;

    // Headers to include key, policy, amazon-credentials, etc..
    private final Map<String, String> headers;

    // The final S3 object key the client should persist
    private final String s3ObjectKey;
    private final String contentType;
    private final long maxBytes;
}
