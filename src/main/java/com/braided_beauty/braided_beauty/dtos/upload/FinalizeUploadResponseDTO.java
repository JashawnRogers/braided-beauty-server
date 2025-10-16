package com.braided_beauty.braided_beauty.dtos.upload;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FinalizeUploadResponseDTO {
    private String s3ObjectKey;
    private String contentType;
    private long contentLength;
    private boolean valid;
}
