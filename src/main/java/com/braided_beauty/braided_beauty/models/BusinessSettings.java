package com.braided_beauty.braided_beauty.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "business_settings")
@Getter
@Setter
@RequiredArgsConstructor
public class BusinessSettings {
    public static final UUID SINGLETON_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Id
    private UUID id = SINGLETON_ID;

    @Column(name = "company_phone_number")
    private String companyPhoneNumber = "";

    @Column(name = "company_address")
    private String companyAddress = "";

    @Column(name = "company_email")
    private String companyEmail = "";

    @Column(name = "apt_buffer_time")
    private Integer appointmentBufferTime = 0;

    @Column(name = "ambassador_discount_percent")
    private Integer ambassadorDiscountPercent = 0;
}
