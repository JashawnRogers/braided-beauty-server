package com.braided_beauty.braided_beauty.mappers.addOn;

import com.braided_beauty.braided_beauty.dtos.addOn.AddOnRequestDTO;
import com.braided_beauty.braided_beauty.dtos.addOn.AddOnResponseDTO;
import com.braided_beauty.braided_beauty.models.AddOn;
import com.braided_beauty.braided_beauty.models.Appointment;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@AllArgsConstructor
public class AddOnDTOMapper {

    public AddOn create(AddOnRequestDTO dto){
      AddOn addOn = new AddOn();

      if (dto.getName() != null) addOn.setName(dto.getName());
      if (dto.getPrice() != null) addOn.setPrice(dto.getPrice());
      if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
          addOn.setDescription(dto.getDescription());
      }

      return addOn;
    }

    public AddOn update(AddOn target,AddOnRequestDTO dto) {
        if (dto.getName() != null) target.setName(dto.getName());
        if (dto.getPrice() != null) target.setPrice(dto.getPrice());
        if (!Objects.equals(dto.getDescription(), target.getDescription())) {
            target.setDescription(dto.getDescription());
        }

        return target;
    }

    public AddOnResponseDTO toDto(AddOn addOn){
        List<Appointment> appointments = new ArrayList<>();
        return AddOnResponseDTO.builder()
                .id(addOn.getId())
                .name(addOn.getName())
                .price(addOn.getPrice())
                .description(addOn.getDescription())
                .appointments(
                        addOn.getAppointments() == null || addOn.getAppointments().isEmpty() ?
                                appointments : addOn.getAppointments()
                )
                .build();
    }
}
