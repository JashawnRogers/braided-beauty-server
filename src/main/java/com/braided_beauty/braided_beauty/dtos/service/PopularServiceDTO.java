package com.braided_beauty.braided_beauty.dtos.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class PopularServiceDTO {
    private final String serviceName;
    private final int completedCount;
}
