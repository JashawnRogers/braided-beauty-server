package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.addOn.AddOnRequestDTO;
import com.braided_beauty.braided_beauty.dtos.addOn.AddOnResponseDTO;
import com.braided_beauty.braided_beauty.services.AddOnService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/addons")
@AllArgsConstructor
public class AddOnController {
    private final AddOnService addOnService;

    @GetMapping
    public ResponseEntity<List<AddOnResponseDTO>> getAllAddOnsByList() {
        return ResponseEntity.ok(addOnService.getAllAddOnsByList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddOnResponseDTO> getAddOn(@PathVariable UUID id) {
        return ResponseEntity.ok(addOnService.getAddOn(id));
    }
}
