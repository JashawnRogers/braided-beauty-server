package com.braided_beauty.braided_beauty.dtos.upload;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FinalizeUploadRequestDTO {
    @NotBlank
    private String s3ObjectKey;
    @NotBlank
    private String purpose;
    @NotBlank
    private String expectedContentType;
}
