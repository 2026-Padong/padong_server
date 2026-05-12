package com.example.padong_server.domain.upload.service;

import com.example.padong_server.global.config.AwsProperties;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class S3FileUploader {

    private static final String BUSINESS_LICENSE_PREFIX = "business-license";
    private static final String PROFILE_PICTURE_PREFIX = "profile-picture";
    private static final String STORE_THUMBNAIL_PREFIX = "store-thumbnail";
    private static final String STORE_IMAGE_PREFIX = "store-image";
    private static final long PROFILE_PICTURE_MAX_BYTES = 5L * 1024 * 1024;
    private static final long STORE_IMAGE_MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> PROFILE_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final S3Client s3Client;
    private final AwsProperties props;
    private final long maxBytes;
    private final Set<String> allowedContentTypes;

    public S3FileUploader(S3Client s3Client, AwsProperties props) {
        this.s3Client = s3Client;
        this.props = props;
        this.maxBytes = props.s3().upload().maxSizeMb() * 1024L * 1024L;
        this.allowedContentTypes =
                Set.of(props.s3().upload().allowedContentTypes().split(","));
    }

    public String uploadBusinessLicense(MultipartFile file, Long userId) {
        validate(file);
        String key =
                BUSINESS_LICENSE_PREFIX
                        + "/"
                        + userId
                        + "/"
                        + UUID.randomUUID()
                        + extOf(file.getContentType());
        putObject(key, file);
        return publicUrl(key);
    }

    public String uploadProfilePicture(MultipartFile file, Long userId) {
        validateProfilePicture(file);
        String key =
                PROFILE_PICTURE_PREFIX
                        + "/"
                        + userId
                        + "/"
                        + UUID.randomUUID()
                        + extOf(file.getContentType());
        putObject(key, file);
        return publicUrl(key);
    }

    public String uploadStoreThumbnail(MultipartFile file, Long storeId) {
        validateStoreImage(file);
        String key =
                STORE_THUMBNAIL_PREFIX
                        + "/"
                        + storeId
                        + "/"
                        + UUID.randomUUID()
                        + extOf(file.getContentType());
        putObject(key, file);
        return publicUrl(key);
    }

    public String uploadStoreImage(MultipartFile file, Long storeId) {
        validateStoreImage(file);
        String key =
                STORE_IMAGE_PREFIX
                        + "/"
                        + storeId
                        + "/"
                        + UUID.randomUUID()
                        + extOf(file.getContentType());
        putObject(key, file);
        return publicUrl(key);
    }

    /** URL 이 우리 S3 버킷 객체면 삭제. 외부 CDN (카카오 등) URL 이면 무시. */
    public void deleteIfOwned(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        String prefix = bucketUrlPrefix();
        if (!url.startsWith(prefix)) {
            return;
        }
        String key = url.substring(prefix.length());
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(props.s3().bucket())
                            .key(key)
                            .build());
        } catch (S3Exception ignored) {
            // 본 객체가 이미 없거나 권한 이슈 — 최선 노력 삭제, 새 업로드는 이미 성공한 상태라 무시
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_UPLOAD_FILE, "파일이 비어있습니다.");
        }
        if (file.getSize() > maxBytes) {
            throw new CustomException(
                    ErrorCode.INVALID_UPLOAD_FILE,
                    "파일 크기가 너무 큽니다. 최대 " + (maxBytes / 1024 / 1024) + "MB 까지 허용합니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new CustomException(
                    ErrorCode.INVALID_UPLOAD_FILE, "허용되지 않는 파일 형식입니다: " + contentType);
        }
    }

    private void validateStoreImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_UPLOAD_FILE, "파일이 비어있습니다.");
        }
        if (file.getSize() > STORE_IMAGE_MAX_BYTES) {
            throw new CustomException(
                    ErrorCode.INVALID_UPLOAD_FILE,
                    "가게 이미지는 최대 "
                            + (STORE_IMAGE_MAX_BYTES / 1024 / 1024)
                            + "MB 까지 업로드 가능합니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !PROFILE_IMAGE_TYPES.contains(contentType)) {
            throw new CustomException(
                    ErrorCode.INVALID_UPLOAD_FILE,
                    "가게 이미지는 image/jpeg, image/png, image/webp 만 허용합니다: " + contentType);
        }
    }

    private void validateProfilePicture(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_UPLOAD_FILE, "파일이 비어있습니다.");
        }
        if (file.getSize() > PROFILE_PICTURE_MAX_BYTES) {
            throw new CustomException(
                    ErrorCode.INVALID_UPLOAD_FILE,
                    "프로필 사진은 최대 "
                            + (PROFILE_PICTURE_MAX_BYTES / 1024 / 1024)
                            + "MB 까지 업로드 가능합니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !PROFILE_IMAGE_TYPES.contains(contentType)) {
            throw new CustomException(
                    ErrorCode.INVALID_UPLOAD_FILE,
                    "프로필 사진은 image/jpeg, image/png, image/webp 만 허용합니다: " + contentType);
        }
    }

    private void putObject(String key, MultipartFile file) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(props.s3().bucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | S3Exception e) {
            throw new CustomException(ErrorCode.UPLOAD_FAILED, e.getMessage());
        }
    }

    private String publicUrl(String key) {
        return bucketUrlPrefix() + key;
    }

    private String bucketUrlPrefix() {
        return "https://"
                + props.s3().bucket()
                + ".s3."
                + props.region()
                + ".amazonaws.com/";
    }

    private String extOf(String contentType) {
        if (contentType == null) {
            return "";
        }
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }
}
