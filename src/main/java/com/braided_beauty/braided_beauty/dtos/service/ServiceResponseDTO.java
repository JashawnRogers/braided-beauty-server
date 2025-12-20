package com.braided_beauty.braided_beauty.dtos.service;

import com.braided_beauty.braided_beauty.dtos.addOn.AddOnResponseDTO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class ServiceResponseDTO {
    private UUID id;
    private String categoryName;
    private UUID categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal depositAmount;
    private Integer durationMinutes;
    private Integer pointsEarned;
    private List<String> photoKeys;
    private String videoKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AddOnResponseDTO> addOns;

}
