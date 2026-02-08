package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.user.admin.UserSummaryResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberProfileResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberRequestDTO;
import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.services.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all-users")
    public ResponseEntity<Page<UserSummaryResponseDTO>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtTo,
            @RequestParam(required = false) UserType userType,
            Pageable pageable
    ) {
        try {
            Page<UserSummaryResponseDTO> page = userService.getAllUsers(search, createdAtFrom, createdAtTo, userType, pageable);
            return ResponseEntity.ok(page);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}")
    public ResponseEntity<UserMemberProfileResponseDTO> updateMemberDataByAdmin(@PathVariable UUID userId, @RequestBody UserMemberRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateMemberDataByAdmin(userId, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}")
    public ResponseEntity<UserMemberProfileResponseDTO> getMemberById(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getMemberById(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/roles/{userId}")
    public ResponseEntity<UserSummaryResponseDTO> updateUserRole(
            @Parameter(description = "UUID of the user", required = true)
            @PathVariable UUID userId,
            @RequestBody UserType userType) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.updateUserRole(userId, userType));
    }
}
