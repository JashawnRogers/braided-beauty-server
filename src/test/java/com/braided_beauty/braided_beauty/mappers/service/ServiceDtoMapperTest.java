package com.braided_beauty.braided_beauty.mappers.service;

import com.braided_beauty.braided_beauty.dtos.service.ServiceCreateDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceRequestDTO;
import com.braided_beauty.braided_beauty.mappers.addOn.AddOnDTOMapper;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServiceDtoMapperTest {

    private final ServiceDtoMapper mapper = new ServiceDtoMapper(new AddOnDTOMapper());

    @Test
    void create_normalizesDescriptionButPreservesLineBreaks() {
        ServiceCreateDTO dto = new ServiceCreateDTO();
        dto.setDescription("  First line  \r\nSecond line   \r\n\r\n");

        ServiceModel service = mapper.create(dto);

        assertEquals("First line\nSecond line", service.getDescription());
    }

    @Test
    void update_clearsDescriptionWhenOnlyWhitespaceIsProvided() {
        ServiceModel service = new ServiceModel();
        service.setDescription("Existing");

        ServiceRequestDTO dto = new ServiceRequestDTO();
        dto.setDescription("   \r\n   ");

        mapper.updateDto(dto, service);

        assertNull(service.getDescription());
    }
}
