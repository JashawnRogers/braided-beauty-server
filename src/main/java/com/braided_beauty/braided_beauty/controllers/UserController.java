package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltyRecordResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.admin.UserAdminAnalyticsDTO;
import com.braided_beauty.braided_beauty.dtos.user.admin.UserSummaryResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberProfileResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberRequestDTO;
import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "Get member profile",
            description = "Returns the profile information of the currently logged-in user or a user by ID (if ADMIN)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserMemberProfileResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the user's profile and not an admin")
    })

    // For testing - TEMPORARY
    //@PreAuthorize("#userId == principal.id or hasRole('ADMIN')") // Allows for admin to access all member profiles
    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserMemberProfileResponseDTO> getMemberProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserPrincipal principal,
            @Parameter(description = "UUID of the user", required = true) @P("userId")@PathVariable UUID userId) {
        try{
            return ResponseEntity.ok(userService.getMemberProfile(userId));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    @Operation(
            summary = "Get appointment history",
            description = "Returns a list of all past appointments for the currently logged-in user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointments retrieved",
                    content = @Content(schema = @Schema(implementation = AppointmentResponseDTO.class)))
    })
    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointmentHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(userService.getAppointmentHistory(principal
                .getId()));
    }

    @Operation(
            summary = "Get loyalty points",
            description = "Retrieves loyalty point records for the logged-in user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loyalty points retrieved",
                    content = @Content(schema = @Schema(implementation = LoyaltyRecordResponseDTO.class)))
    })
    @GetMapping("/loyalty-points")
    public ResponseEntity<LoyaltyRecordResponseDTO> getLoyaltyPoints(
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(userService.getLoyaltyPoints(principal
                .getId()));
    }

    @Operation(
            summary = "Get users (paged, ADMIN only)",
            description = """
        Returns a paginated list of users. Supports full-text search (name/email),
        creation date range, user type filtering, and sorting.
        Only accessible by admins.
        """
    )
    @Parameters({
            @Parameter(
                    name = "search",
                    in = ParameterIn.QUERY,
                    description = "Full-text search (matches name or email, case-insensitive).",
                    example = "john"
            ),
            @Parameter(
                    name = "createdAtFrom",
                    in = ParameterIn.QUERY,
                    description = "Filter users created at/after this instant (ISO 8601).",
                    example = "2025-01-01T00:00:00Z",
                    schema = @Schema(type = "string", format = "date-time")
            ),
            @Parameter(
                    name = "createdAtTo",
                    in = ParameterIn.QUERY,
                    description = "Filter users created at/before this instant (ISO 8601).",
                    example = "2025-12-31T23:59:59Z",
                    schema = @Schema(type = "string", format = "date-time")
            ),
            @Parameter(
                    name = "userType",
                    in = ParameterIn.QUERY,
                    description = "User type/role filter.",
                    example = "ADMIN",
                    schema = @Schema(implementation = UserType.class)
            ),
            // Pageable params (expanded by springdoc using @ParameterObject)
            @Parameter(
                    name = "page",
                    in = ParameterIn.QUERY,
                    description = "Zero-based page index (defaults to 0).",
                    example = "0"
            ),
            @Parameter(
                    name = "size",
                    in = ParameterIn.QUERY,
                    description = "Page size (defaults to 25).",
                    example = "25"
            ),
            @Parameter(
                    name = "sort",
                    in = ParameterIn.QUERY,
                    description = "Sorting criteria in the format `property,ASC|DESC`. Repeat to sort by multiple fields.",
                    example = "createdAt,DESC"
            )
    })
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UserSummaryResponseDTO.class))
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    //@PreAuthorize("hasRole('ADMIN')")
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


    @Operation(
            summary = "Get monthly analytics (ADMIN only)",
            description = "Returns analytics data for the given month. Only accessible by admins.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Month and year to retrieve analytics for (format: YYYY-MM)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = YearMonth.class))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics retrieved",
                    content = @Content(schema = @Schema(implementation = UserAdminAnalyticsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/analytics")
    public ResponseEntity<UserAdminAnalyticsDTO> getAnalytics(@RequestBody YearMonth yearMonth) {
        return ResponseEntity.ok(userService.getAnalytics(yearMonth));
    }

    @Operation(
            summary = "Update user role (ADMIN only)",
            description = "Updates the role (e.g., MEMBER, ADMIN) for a specific user.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New user role",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserType.class))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Role updated successfully",
                    content = @Content(schema = @Schema(implementation = UserSummaryResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/roles/{userId}")
    public ResponseEntity<UserSummaryResponseDTO> updateUserRole(
            @Parameter(description = "UUID of the user", required = true)
            @PathVariable UUID userId,
            @RequestBody UserType userType) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.updateUserRole(userId, userType));
    }

    //@PreAuthorize("#userId == principal.id or hasRole('ADMIN')")
    @PutMapping("/{userId}")
    public ResponseEntity<UserMemberProfileResponseDTO> updateMemberData(@PathVariable UUID userId, @RequestBody UserMemberRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateMemberData(dto));
    }
}
