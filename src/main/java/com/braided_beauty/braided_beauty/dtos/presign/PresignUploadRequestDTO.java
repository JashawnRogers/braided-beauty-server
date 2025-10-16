package com.braided_beauty.braided_beauty.dtos.presign;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PresignUploadRequestDTO {

    // The original client filename (used only to pick an extension; not trusted)
    @NotBlank
    private String fileName;

    // Browser reported MIME (also validated on server)
    @NotBlank
    private String contentType;

    // "service-photo" | "service-video" (to choose key prefix and constraints)
    @NotBlank
    private String purpose;

    // Optional - use when attaching to an existing service
    private UUID serviceId;
}
