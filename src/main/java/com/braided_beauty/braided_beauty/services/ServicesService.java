package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.service.ServiceCreateDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceRequestDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.exceptions.DuplicateEntityException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.service.ServiceDtoMapper;
import com.braided_beauty.braided_beauty.models.AddOn;
import com.braided_beauty.braided_beauty.models.ServiceCategory;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.repositories.AddOnRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceCategoryRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ServicesService {
    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final ServiceDtoMapper serviceDtoMapper;
    private final AddOnRepository addOnRepository;
    private final MediaService mediaService;

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
            ServiceCategory categoryProxy = categoryRepository.getReferenceById(dto.getCategoryId());
            entity.setCategory(categoryProxy);
        }

        if (dto.getAddOnIds() != null && !dto.getAddOnIds().isEmpty()) {
            List<AddOn> addOnProxies = dto.getAddOnIds().stream()
                    .map(addOnRepository::getReferenceById).toList();

            entity.setAddOns(addOnProxies);
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
    public ServiceResponseDTO updateService(UUID serviceId, ServiceRequestDTO dto){
        ServiceModel existingService = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found."));

        List<String> beforeKeys = existingService.getPhotoKeys() == null
                ? List.of()
                : List.copyOf(existingService.getPhotoKeys());

        serviceDtoMapper.updateDto(dto, existingService);

        if (dto.getAddOnIds() != null && !dto.getAddOnIds().isEmpty()) {
            existingService.setAddOns(addOnRepository.findAllById(dto.getAddOnIds()));
        }

        if (dto.getRemovePhotoKeys() != null && !dto.getRemovePhotoKeys().isEmpty()) {
            var beforeSet = new HashSet<>(beforeKeys);
            for (String key : dto.getRemovePhotoKeys()) {
                if (beforeSet.contains(key)) {
                    try {
                        mediaService.delete(key);
                    } catch (Exception e) {
                        log.warn("S3 deletion failed for key={}", key, e);
                    }
                }
            }
        }

        ServiceModel saved = serviceRepository.save(existingService);
        log.info("Updated service with ID: {}", serviceId);
        return serviceDtoMapper.toDto(saved);
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
                .map(serviceDtoMapper::toDto)
                .map(dto -> {attachPhotoUrls(dto); return dto; });
    }

    public List<ServiceResponseDTO> getAllServicesByList() {
        return serviceRepository.findAll()
                .stream()
                .map(serviceDtoMapper::toDto)
                .peek(this::attachPhotoUrls)
                .toList();
    }

    public ServiceResponseDTO getServiceById(UUID serviceId){
        ServiceModel service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found."));

        ServiceResponseDTO dto = serviceDtoMapper.toDto(service);

        attachPhotoUrls(dto);

        log.info("Retrieved service with ID: {}", serviceId);
        return dto;
    }
    public List<ServiceResponseDTO> getAllServicesByCategory(UUID categoryId) {
       return serviceRepository.findAllByCategoryId(categoryId)
                .orElseThrow(() -> new NotFoundException("No services found under this category"))
                .stream()
                .map(serviceDtoMapper::toDto)
                .peek(this::attachPhotoUrls)
                .toList();
    }

    private void attachPhotoUrls(ServiceResponseDTO dto) {
        List<String> keys = dto.getPhotoKeys();

        if (keys == null || keys.isEmpty()) {
            return;
        }


        try {
            List<String> urls = keys.stream()
                            .map(url -> mediaService.presignGet(url, Duration.ofMinutes(60)))
                            .toList();
            dto.setCoverImageUrl(urls.getFirst());
            dto.setPhotoUrls(urls);
        } catch (Exception e) {
            log.warn("Failed to presign cover image for keys");
            dto.setCoverImageUrl(null);
            dto.setPhotoUrls(null);
        }

    }
}
