package com.braided_beauty.braided_beauty.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
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

    @Column(nullable = false, length = 120)
    private String name;
    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "deposit_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @ElementCollection
    @CollectionTable(name ="service_keys", joinColumns = @JoinColumn(name = "service_id"))
    @Column(name = "url", length = 512)
    @OrderColumn(name = "position")
    private List<String> photoKeys = new ArrayList<>();

    @Column(name = "video_key")
    private String videoKey;

    @OneToMany(mappedBy = "service")
    @OrderBy("startAt DESC")
    private List<Appointment> appointments;

    @Column(name ="points_earned", nullable = false)
    private Integer pointsEarned = 0;

    @Column(name = "times_booked")
    private Integer timesBooked = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
