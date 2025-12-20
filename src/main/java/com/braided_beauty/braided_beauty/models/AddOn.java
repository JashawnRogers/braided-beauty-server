package com.braided_beauty.braided_beauty.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "add_on")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"appointments", "services"}) // To prevent circular dependencies in Java
@ToString(exclude = {"appointments", "services"}) // To prevent circular dependencies in Java
@Builder
public class AddOn {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @JsonIgnore // Prevents circular dependencies when serializing the object for exporting to other platforms
    @ManyToMany(mappedBy = "addOns")
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    @ManyToMany(mappedBy = "addOns")
    @Builder.Default
    @JsonIgnore
    private List<ServiceModel> services = new ArrayList<>();

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;
}
