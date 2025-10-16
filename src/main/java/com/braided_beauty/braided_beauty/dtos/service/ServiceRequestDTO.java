package com.braided_beauty.braided_beauty.dtos.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


/**
 * Used for UPDATE/PATCH
 * Any null field = no change
 * non-null strings can be blank to explicitly clear (see mapper).
 */

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceRequestDTO {

    private UUID serviceId;
    @Size(min = 1, max = 150, message = "Name must be at least 1 character and more than 150.")
    private String name;
    @Size(max = 500, message = "Description must be no more than 500 characters.")
    private String Description;
    @DecimalMin(value = "0.00", message = "Price must not be a negative number.")
    private BigDecimal price;
    @DecimalMin(value = "0.00", message = "Deposit amount must not be a negative number.")
    private BigDecimal depositAmount;
    @Min(value = -1, message = "Duration of service must not be a negative number.")
    private Integer durationMinutes;

    // Media state deltas:
    // If null -> leave unchanged - If present -> apply changes
    @Nullable
    private List<String> photoKeys;
    @Nullable
    private List<String> addPhotoKeys;
    @Nullable
    private List<String> removePhotoKeys;

    /**
     * - null -> leave unchanged
     * - "" (blank) -> clear video
     * - non-blank -> set/replace with video key
     */
    @Nullable
    private String videoKey;
}
