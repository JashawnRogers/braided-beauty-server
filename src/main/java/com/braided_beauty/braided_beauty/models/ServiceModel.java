package com.braided_beauty.braided_beauty.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "services")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ServiceModel {
    @Id
    @GeneratedValue
    private UUID id;

    private String name;
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "video_url")
    private String videoUrl;

    @OneToMany(mappedBy = "serviceModel")
    private List<Appointment> appointments;

    @Column(name = "deposit_amount", nullable = false)
    private BigDecimal depositAmount;

    @Column(name ="points_earned", nullable = false)
    private Integer pointsEarned;
}
