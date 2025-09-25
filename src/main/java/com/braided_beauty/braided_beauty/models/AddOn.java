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
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = "appointments") // To prevent circular dependencies in Java
@ToString(exclude = "appointments") // To prevent circular dependencies in Java
@Builder
public class AddOn {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;
    @Column(name = "name")
    private String name;
    @Column(name = "price")
    private BigDecimal price;
    @JsonIgnore // Prevents circular dependencies when serializing the object for exporting to other platforms
    @ManyToMany(mappedBy = "addOns")
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();
}
