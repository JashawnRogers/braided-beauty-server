package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.models.AddOn;
import com.braided_beauty.braided_beauty.repositories.AddOnRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Getter
@Setter
public class AddOnService {
    private final AddOnRepository addOnRepository;
    private final AppointmentRequestDTO appointmentRequestDTO;

    public List<AddOn> getAddOnIds(List<UUID> ids){
       if (ids == null || ids.isEmpty()){
           return List.of();
       }

       return addOnRepository.findAllById(ids);
    }
}
