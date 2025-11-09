package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.service.ServiceCreateDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceRequestDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.exceptions.DuplicateEntityException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.service.ServiceDtoMapper;
import com.braided_beauty.braided_beauty.models.ServiceCategory;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.repositories.ServiceCategoryRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ServicesService {
    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final ServiceDtoMapper serviceDtoMapper;
    private final static Logger log = LoggerFactory.getLogger(ServicesService.class);

    @Transactional
    public ServiceResponseDTO createService(ServiceCreateDTO dto){
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("A service name must be provided to create a service");
        }

        if (dto.getPrice() == null) {
            throw new IllegalArgumentException("Price is required");
        }

        if (serviceRepository.existsByName(dto.getName().trim())){
            throw new DuplicateEntityException("A service with this name already exists.");
        }

        ServiceModel entity = serviceDtoMapper.create(dto);
        if (dto.getCategoryId() != null) {
            ServiceCategory category = categoryRepository.getReferenceById(dto.getCategoryId());
            entity.setCategory(category);
        }


        ServiceModel saved = serviceRepository.save(entity);
        log.info("Created service with ID: {}", saved.getId());
        return serviceDtoMapper.toDto(saved);
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

    public Page<ServiceResponseDTO> getAllServices(String name, Pageable pageable){
        Specification<ServiceModel> spec =
                (root, query, criteriaBuilder)
                        -> criteriaBuilder.conjunction();

        if (name != null && !name.isBlank()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%")
            ));
        }
        return serviceRepository.findAll(spec, pageable)
                .map(serviceDtoMapper::toDto);
    }

    public ServiceResponseDTO getServiceById(UUID serviceId){
        ServiceModel service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found."));
        log.info("Retrieved service with ID: {}", serviceId);
        return serviceDtoMapper.toDto(service);
    }
}
