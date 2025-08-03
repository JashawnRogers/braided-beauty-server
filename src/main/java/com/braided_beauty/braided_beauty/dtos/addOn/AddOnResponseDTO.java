package com.braided_beauty.braided_beauty.dtos.addOn;

import com.braided_beauty.braided_beauty.models.Appointment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class AddOnResponseDTO {
    private final UUID id;
    private final String name;
    private final BigDecimal price;
    private final List<Appointment> appointments;
}
