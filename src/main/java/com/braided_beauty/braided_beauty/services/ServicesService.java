package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.service.ServiceCreateDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceRequestDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.exceptions.DuplicateEntityException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.service.ServiceDtoMapper;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ServicesService {
    private final ServiceRepository serviceRepository;
    private final ServiceDtoMapper serviceDtoMapper;
    private final static Logger log = LoggerFactory.getLogger(ServicesService.class);

    @Transactional
    public ServiceResponseDTO createService(ServiceCreateDTO dto){
        ServiceModel service = serviceDtoMapper.create(dto);
        if (serviceRepository.existsByName(service.getName())){
            throw new DuplicateEntityException("A service with this name already exists.");
        }
        log.info("Created service with ID: {}", service.getId());
        return serviceDtoMapper.toDto(service);
    }

    @Transactional
    public void deleteServiceById(UUID serviceId){
        ServiceModel serviceToDelete = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found."));
        serviceRepository.delete(serviceToDelete);
        log.info("Deleting service with ID: {} ", serviceId);
    }

    @Transactional
    public ServiceResponseDTO updateService(UUID serviceId, ServiceRequestDTO serviceRequestDTO){
        ServiceModel existingService = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found."));
        serviceDtoMapper.updateDto(serviceRequestDTO, existingService);
        log.info("Updated service with ID: {}", serviceId);
        return serviceDtoMapper.toDto(existingService);
    }

    public List<ServiceResponseDTO> getAllServices(){
        List<ServiceModel> allServices = serviceRepository.findAll();
        log.info("Returning all services: {}", allServices.size());
        return allServices.stream()
                .map(serviceDtoMapper::toDto)
                .toList();
    }

    public ServiceResponseDTO getServiceById(UUID serviceId){
        ServiceModel service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found."));
        log.info("Retrieved service with ID: {}", serviceId);
        return serviceDtoMapper.toDto(service);
    }
}
