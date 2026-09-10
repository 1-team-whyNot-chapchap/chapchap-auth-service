package com.chapchap.auth.global.service.storage;

import com.chapchap.auth.global.config.storage.MinioConfig;

import com.chapchap.auth.global.error.custom.business.InvalidParameterException;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MinioManagerTest {
    private final MinioManager manager = new MinioManager(
            new MinioConfig("http://minio.test", "bucket", "key", "secret", "profiles/users", List.of()),
            mock(MinioClient.class)
    );

    @Test
    void acceptsJpegOnlyWhenDeclaredMimeTypeMatchesBinarySignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "anything.png", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );

        assertThat(manager.validateProfileImage(file)).isEqualTo("jpg");
    }

    @Test
    void rejectsGifAndMimeTypeSpoofing() {
        MockMultipartFile gif = new MockMultipartFile(
                "file", "profile.gif", "image/gif", new byte[]{'G', 'I', 'F', '8'}
        );
        MockMultipartFile spoofed = new MockMultipartFile(
                "file", "profile.jpg", "image/png", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        assertThatThrownBy(() -> manager.validateProfileImage(gif)).isInstanceOf(InvalidParameterException.class);
        assertThatThrownBy(() -> manager.validateProfileImage(spoofed)).isInstanceOf(InvalidParameterException.class);
    }
}
