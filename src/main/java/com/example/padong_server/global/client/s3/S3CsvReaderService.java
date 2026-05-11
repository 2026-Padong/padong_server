package com.example.padong_server.global.client.s3;

import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.io.InputStream;
import java.text.Normalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3CsvReaderService {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public InputStream readCsv(String domain, String filename) {
        String bucket = s3Properties.bucket();
        String key = buildKey(domain, filename);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try {
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            return response;
        } catch (NoSuchKeyException e) {
            log.warn("S3 CSV 읽기 실패: bucket={}, key={}, error={}", bucket, key, e.awsErrorDetails().errorMessage());
            throw new CustomException(ErrorCode.S3_CSV_NOT_FOUND, "S3 CSV 파일을 찾을 수 없습니다: " + key, e);
        } catch (S3Exception e) {
            log.error("S3 CSV 읽기 실패: bucket={}, key={}, error={}", bucket, key, e.awsErrorDetails().errorMessage(), e);
            throw e;
        }
    }

    String buildKey(String domain, String filename) {
        String normalizedDomain = normalizePathPart(domain, "domain");
        String normalizedFilename = normalizePathPart(filename, "filename");
        String prefix = stripSlashes(s3Properties.prefix());

        if (!StringUtils.hasText(prefix)) {
            return normalizedDomain + "/" + normalizedFilename;
        }
        return prefix + "/" + normalizedDomain + "/" + normalizedFilename;
    }

    private String normalizePathPart(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return stripSlashes(Normalizer.normalize(value.trim(), Normalizer.Form.NFD));
    }

    private String stripSlashes(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
