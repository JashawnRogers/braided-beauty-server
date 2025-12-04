package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltyRecordResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.global.CurrentUserDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UpdateMemberProfileDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserDashboardDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberProfileResponseDTO;
import com.braided_beauty.braided_beauty.exceptions.UnauthorizedException;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.services.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointmentHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(userService.getAppointmentHistory(principal
                .id()));
    }

    @GetMapping("/loyalty-points")
    public ResponseEntity<LoyaltyRecordResponseDTO> getLoyaltyPoints(
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(userService.getLoyaltyPoints(principal
                .id()));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<CurrentUserDTO> updateMemberDataByMember(
            @AuthenticationPrincipal AppUserPrincipal principal, @RequestBody UpdateMemberProfileDTO dto) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateMemberDataByMember(principal.id(), dto));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<CurrentUserDTO> getUserProfile(@AuthenticationPrincipal AppUserPrincipal principal){
        return ResponseEntity.ok(userService.getMemberProfile(principal.id()));
    }

    @GetMapping("/dashboard/me")
    public ResponseEntity<UserDashboardDTO> getMyDashboard(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        if (principal == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return ResponseEntity.ok(userService.getDashboard(principal.id()));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserDTO> getCurrentUser(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.id()));
    }
}
