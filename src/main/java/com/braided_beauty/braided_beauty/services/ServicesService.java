package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.service.ServicePatchDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceRequestDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.exceptions.DuplicateEntityException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.service.ServiceDtoMapper;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;


@Service
@AllArgsConstructor
public class ServicesService {
    private final ServiceRepository serviceRepository;
    private final ServiceDtoMapper serviceDtoMapper;
    private final static Logger log = LoggerFactory.getLogger(ServicesService.class);

    public ServiceResponseDTO createService(ServiceRequestDTO serviceRequestDTO){
        ServiceModel service = serviceDtoMapper.toEntity(serviceRequestDTO);
        if (serviceRepository.existsByName(service.getName())){
            throw new DuplicateEntityException("A service with this name already exists.");
        }
        ServiceModel newService = serviceRepository.save(service);
        log.info("Created service with ID: {}", newService.getId());
        return serviceDtoMapper.toDTO(newService);
    }

    public void deleteService(UUID serviceId){
        ServiceModel serviceToDelete = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found."));
        serviceRepository.delete(serviceToDelete);
        log.info("Deleting service with ID: {}, ", serviceId);
    }

    public ServiceResponseDTO updateService(UUID serviceId, ServicePatchDTO servicePatchDTO){
        ServiceModel existingService = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found."));
        serviceDtoMapper.updateServiceFromPatchDTO(servicePatchDTO, existingService);
        ServiceModel updatedService = serviceRepository.save(existingService);
        log.info("Updated service with ID: {}", serviceId);
        return serviceDtoMapper.toDTO(updatedService);
    }

    public List<ServiceResponseDTO> getAllServices(){
        List<ServiceModel> allServices = serviceRepository.findAll();
        log.info("Returning all services: {}", allServices.size());
        return allServices.stream()
                .map(serviceDtoMapper::toDTO)
                .toList();
    }

    public ServiceResponseDTO getServiceById(UUID serviceId){
        ServiceModel service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found."));
        log.info("Retrieved service with ID: {}", serviceId);
        return serviceDtoMapper.toDTO(service);
    }
}
