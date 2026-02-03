package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.presign.PresignPutResponseDTO;
import com.braided_beauty.braided_beauty.dtos.presign.PresignUploadRequestDTO;
import com.braided_beauty.braided_beauty.dtos.upload.FinalizeUploadRequestDTO;
import com.braided_beauty.braided_beauty.dtos.upload.FinalizeUploadResponseDTO;
import com.braided_beauty.braided_beauty.services.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MediaController {
    private final MediaService mediaService;

    @PostMapping("/presign-put")
    public ResponseEntity<PresignPutResponseDTO> presign(@Valid @RequestBody PresignUploadRequestDTO req) {
        return ResponseEntity.ok(mediaService.presignPut(req));
    }

    @PostMapping("/finalize")
    public ResponseEntity<FinalizeUploadResponseDTO> finalize(@Valid @RequestBody FinalizeUploadRequestDTO dto) {
        return ResponseEntity.ok(mediaService.verifyAndFinalize(dto));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam("key") String key) {
        mediaService.delete(key);
        return ResponseEntity.noContent().build();
    }
}
