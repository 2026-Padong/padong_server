package com.example.padong_server.global.client.s3;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.padong_server.global.config.AwsProperties;
import java.text.Normalizer;
import org.junit.jupiter.api.Test;

class S3CsvReaderServiceTest {

    private static AwsProperties props(String bucket, String prefix) {
        return new AwsProperties(
                "ap-northeast-2", null, null, new AwsProperties.S3(bucket, prefix, null));
    }

    @Test
    void buildKeyNormalizesKoreanFilenameToNfdAndCombinesPrefixDomainFilename() {
        S3CsvReaderService service = new S3CsvReaderService(null, props("padong", "padongBE"));

        String key = service.buildKey("dongne", "행정동.csv");

        assertThat(key).isEqualTo("padongBE/dongne/" + Normalizer.normalize("행정동.csv", Normalizer.Form.NFD));
        assertThat(Normalizer.isNormalized(key.substring("padongBE/dongne/".length()), Normalizer.Form.NFD)).isTrue();
    }

    @Test
    void buildKeyDoesNotCreateDuplicateSlashesWhenPrefixHasSlashes() {
        S3CsvReaderService service = new S3CsvReaderService(null, props("padong", "/padongBE/"));

        String key = service.buildKey("/population/", "/인구.csv/");

        assertThat(key).isEqualTo("padongBE/population/" + Normalizer.normalize("인구.csv", Normalizer.Form.NFD));
    }
}
