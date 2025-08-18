package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltyRecordResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.admin.UserAdminAnalyticsDTO;
import com.braided_beauty.braided_beauty.dtos.user.admin.UserSummaryResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberProfileResponseDTO;
import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @PreAuthorize("#userId == principal.id or hasRole('ADMIN')") // Allows for admin to access all member profiles
    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserMemberProfileResponseDTO> getMemberProfile(@AuthenticationPrincipal AppUserPrincipal principal,
                                                                         @PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getMemberProfile(userId));
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointmentHistory(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(userService.getAppointmentHistory(principal
                .getId()));
    }

    @GetMapping("/loyalty-points")
    public ResponseEntity<LoyaltyRecordResponseDTO> getLoyaltyPoints(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(userService.getLoyaltyPoints(principal
                .getId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all-users")
    public ResponseEntity<List<UserSummaryResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/analytics")
    public ResponseEntity<UserAdminAnalyticsDTO> getAnalytics(@RequestBody YearMonth yearMonth) {
        return ResponseEntity.ok(userService.getAnalytics(yearMonth));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/roles/{userId}")
    public ResponseEntity<UserSummaryResponseDTO> updateUserRole(@PathVariable UUID userId, @RequestBody UserType userType) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.updateUserRole(userId, userType));
    }
}
