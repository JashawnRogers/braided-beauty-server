package com.braided_beauty.braided_beauty.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    @ElementCollection
    @CollectionTable(name ="service_key", joinColumns = @JoinColumn(name = "service_id"))
    @Column(name = "url")
    private List<String> photoKeys = new ArrayList<>();

    @Column(name = "video_key")
    private String videoKey;

    @OneToMany(mappedBy = "service")
    private List<Appointment> appointments;

    @Column(name = "deposit_amount", nullable = false)
    private BigDecimal depositAmount;

    @Column(name ="points_earned", nullable = false)
    private Integer pointsEarned;

    @Column(name = "times_booked")
    private Integer timesBooked;
}
