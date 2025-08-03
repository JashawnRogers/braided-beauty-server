package com.braided_beauty.braided_beauty.mappers.addOn;

import com.braided_beauty.braided_beauty.dtos.addOn.AddOnRequestDTO;
import com.braided_beauty.braided_beauty.dtos.addOn.AddOnResponseDTO;
import com.braided_beauty.braided_beauty.models.AddOn;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class AddOnDTOMapper {

    public AddOn toEntity(AddOnRequestDTO dto){
        return AddOn.builder()
                .id(dto.getId())
                .name(dto.getName())
                .price(dto.getPrice())
                .build();
    }

    public AddOnResponseDTO toDto(AddOn addOn){
        return AddOnResponseDTO.builder()
                .id(addOn.getId())
                .name(addOn.getName())
                .price(addOn.getPrice())
                .appointments(!addOn.getAppointments().isEmpty() ? addOn.getAppointments() : List.of() )
                .build();
    }
}
