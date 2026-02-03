package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.presign.PresignPutResponseDTO;
import com.braided_beauty.braided_beauty.dtos.presign.PresignUploadRequestDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.dtos.upload.FinalizeUploadRequestDTO;
import com.braided_beauty.braided_beauty.dtos.upload.FinalizeUploadResponseDTO;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.records.S3Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.util.*;

@Service
public class MediaService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private final String bucket;
    private final long maxImageBytes;
    private final long maxVideoBytes;

    public MediaService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            S3Properties S3props,
            @Value("${app.media.maxImageBytes}") long maxImageBytes,
            @Value("${app.media.maxVideoBytes}") long maxVideoBytes
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = S3props.bucketName();
        this.maxImageBytes = maxImageBytes;
        this.maxVideoBytes = maxVideoBytes;
    }

    private final Set<String> allowedImages = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/heic", "image/heif"
    );
    private final Set<String> allowedVideos = Set.of(
      "video/mp4", "video/quicktime" // quicktime = mov
    );

    public PresignPutResponseDTO presignPut(PresignUploadRequestDTO request) {
        final String contentType = request.getContentType();
        final String purpose = request.getPurpose();

        boolean isImage = "service-photo".equals(purpose);

        if (!isImage) throw new IllegalArgumentException("Unsupported purpose " + purpose);
        if (!allowedImages.contains(contentType))
            throw new IllegalArgumentException("Unsupported image type " + contentType);



        String extension = fileExtensionFrom(contentType, request.getFileName());
        String prefix = "services/photos";
        if (request.getServiceId() != null) {
            prefix = ("services/" + request.getServiceId() + "/photos");
        }
        String key = prefix + "/" + UUID.randomUUID() + extension;
        Duration duration = Duration.ofMinutes(5);


        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(p -> p
                .signatureDuration(duration)
                .putObjectRequest(put));

        URL url = presigned.url();
        Map<String, String> headers = new HashMap<>();
        presigned.signedHeaders().forEach((k, v) -> headers.put(k, String.join(",", v)));

        return PresignPutResponseDTO.builder()
                .s3Url(url.toString())
                .headers(headers)
                .s3ObjectKey(key)
                .contentType(contentType)
                .maxBytes(maxImageBytes)
                .build();

    }

    public String presignGet(String key, Duration duration) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("s3ObjectKey is null/blank");
        }

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(duration)
                        .getObjectRequest(get)
                        .build()
        );

        return presignedGetObjectRequest.url().toString();
    }

    public FinalizeUploadResponseDTO verifyAndFinalize(FinalizeUploadRequestDTO dto) {
        if (dto == null || dto.getS3ObjectKey() == null || dto.getS3ObjectKey().isBlank()) {
            throw new IllegalArgumentException("Missing s3ObjectKey");
        }

        boolean isImage = "service-photo".equals(dto.getPurpose());
        if (!isImage) throw new IllegalArgumentException("Unsupported purpose: " + dto.getPurpose());


        HeadObjectResponse head = s3Client.headObject(b -> b.bucket(bucket).key(dto.getS3ObjectKey()));
        String contentType = head.contentType();
        long length = head.contentLength() == null ? -1L : head.contentLength();

        boolean typeOK = allowedImages.contains(contentType);
        boolean sizeOK = length >= 0 && length <= maxImageBytes;
        boolean expectedOK = dto.getExpectedContentType() != null && dto.getExpectedContentType().equalsIgnoreCase(contentType);
        boolean valid = typeOK && sizeOK && expectedOK;

        if (!valid) {
            s3Client.deleteObject(b -> b.bucket(bucket).key(dto.getS3ObjectKey()));
        }

        return FinalizeUploadResponseDTO.builder()
                .s3ObjectKey(dto.getS3ObjectKey())
                .contentType(dto.getExpectedContentType())
                .contentLength(length)
                .valid(valid)
                .build();

    }

    public void delete(String key) {
        if (key.isBlank()) throw new IllegalArgumentException("S3ObjectKey is null");
        s3Client.deleteObject(b ->b.bucket(bucket).key(key));
    }

    // To pick an extension from MIME or fallback to original filename
    private static String fileExtensionFrom(String contentType, String fileName) {
        if (fileName.isBlank()) throw new IllegalArgumentException("fileName is null");

        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpeg";
            case "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/heic" -> ".heic";
            case "image/heif" -> ".heif";
            case "video/mp4" -> ".mp4";
            case "video/quicktime" -> ".mov";
            default -> null;
        };

        if (extension != null) return extension;
        int dot = fileName.lastIndexOf(".");
        return dot > -1 ? fileName.substring(dot) : "";
    }
}
