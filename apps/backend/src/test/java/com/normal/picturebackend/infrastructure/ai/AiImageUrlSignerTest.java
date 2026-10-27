package com.normal.picturebackend.infrastructure.ai;

import com.normal.picturebackend.infrastructure.config.AiServiceProperties;
import com.normal.picturebackend.infrastructure.config.StorageProperties;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiImageUrlSignerTest {
    private final MinioClient client = mock(MinioClient.class);
    private final StorageProperties storage = new StorageProperties();
    private final AiImageUrlSigner signer = new AiImageUrlSigner(client, storage, new AiServiceProperties());

    @Test
    void signsExactKeyWithShortGetExpiryEvenWhenPublicRead() throws Exception {
        storage.getMinio().setBucket("smart-image");
        storage.getMinio().setPublicRead(true);
        when(client.getPresignedObjectUrl(any())).thenReturn("signed-url");
        assertEquals("signed-url", signer.sign("http://127.0.0.1:9000/smart-image/a%2Bb%2520.png?old=expired"));
        ArgumentCaptor<GetPresignedObjectUrlArgs> args = ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(client).getPresignedObjectUrl(args.capture());
        assertEquals("a+b%20.png", args.getValue().object());
        assertEquals("smart-image", args.getValue().bucket());
        assertEquals(Method.GET, args.getValue().method());
        assertEquals(120, args.getValue().expiry());
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://evil.example/smart-image/a.png", "http://127.0.0.1:9000/other/a.png",
            "http://127.0.0.1:9001/smart-image/a.png", "http://user@127.0.0.1:9000/smart-image/a.png",
            "http://127.0.0.1:9000/smart-image/../secret", "not-a-url"})
    void doesNotSignForeignUrls(String url) {
        storage.getMinio().setBucket("smart-image");
        assertThrows(BusinessException.class, () -> signer.sign(url));
        verifyNoInteractions(client);
    }
}
