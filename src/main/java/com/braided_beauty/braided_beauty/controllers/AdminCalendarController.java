package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.calendar.*;
import com.braided_beauty.braided_beauty.services.AdminAppointmentService;
import com.braided_beauty.braided_beauty.services.ScheduleCalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/calendars")
@RequiredArgsConstructor
public class AdminCalendarController {
    private final ScheduleCalendarService scheduleCalendarService;
    private final AdminAppointmentService adminAppointmentService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<AdminCalendarDTO>> getCalendars() {
        List<AdminCalendarDTO> calendars = scheduleCalendarService.getAdminCalendars()
                .stream()
                .map(scheduleCalendarService::toAdminCalendarDto)
                .toList();

        return ResponseEntity.ok(calendars);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AdminCalendarDTO> createCalendar(@Valid @RequestBody AdminCalendarCreateRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleCalendarService.createCalendar(dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<AdminCalendarDTO> updateCalendar(
            @PathVariable UUID id,
            @RequestBody AdminCalendarUpdateRequestDTO dto
    ) {
        return ResponseEntity.ok(scheduleCalendarService.updateCalendar(id, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCalendar(@PathVariable UUID id) {
        scheduleCalendarService.deleteCalendar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/hours")
    public ResponseEntity<Void> upsertHours(
            @PathVariable UUID id,
            @RequestBody List<@Valid AdminCalendarHoursUpsertDTO> hours
    ) {
        scheduleCalendarService.upsertWeeklyHours(id, hours);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/hours")
    public ResponseEntity<List<AdminCalendarHoursDTO>> getHours(@PathVariable UUID id) {
        return ResponseEntity.ok(scheduleCalendarService.getWeeklyHours(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/overrides")
    public ResponseEntity<List<AdminCalendarOverrideDTO>> getOverrides(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ResponseEntity.ok(scheduleCalendarService.getOverrides(id, start, end));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/overrides")
    public ResponseEntity<List<AdminCalendarOverrideDTO>> upsertOverrides(
            @PathVariable UUID id,
            @RequestBody List<@Valid AdminCalendarOverrideUpsertDTO> overrides
    ) {
        return ResponseEntity.ok(scheduleCalendarService.upsertOverrides(id, overrides));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/overrides/{overrideId}")
    public ResponseEntity<Void> deleteOverride(
            @PathVariable UUID id,
            @PathVariable UUID overrideId
    ) {
        scheduleCalendarService.deleteOverride(id, overrideId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/events")
    public ResponseEntity<List<AdminCalendarEventDTO>> getCalendarEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(adminAppointmentService.getCalendarEvents(start, end));
    }
}
