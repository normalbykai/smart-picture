package com.normal.picturebackend.infrastructure.ai;

import com.normal.picturebackend.infrastructure.config.AiServiceProperties;
import com.normal.picturebackend.infrastructure.config.StorageProperties;
import io.minio.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** 显式启用：真实 Java 签名 -> Python 验证和下载 -> 图片预处理；不调用付费模型。 */
@EnabledIfEnvironmentVariable(named = "AI_STORAGE_LIVE_TEST", matches = "true")
class AiStorageLiveTest {
    @Test
    void pythonDownloadsJavaSignedObject() throws Exception {
        StorageProperties storage = new StorageProperties();
        MinioClient minio = MinioClient.builder().endpoint(storage.getMinio().getEndpoint())
                .credentials(storage.getMinio().getAccessKey(), storage.getMinio().getSecretKey()).build();
        String bucket = storage.getMinio().getBucket();
        if (!minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        String key = "ai-analysis-test/" + UUID.randomUUID() + "+sample.png";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(24, 32, BufferedImage.TYPE_INT_RGB), "png", bytes);
        minio.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                .stream(new ByteArrayInputStream(bytes.toByteArray()), bytes.size(), -1)
                .contentType("image/png").build());
        try {
            String signed = new AiImageUrlSigner(minio, storage, new AiServiceProperties())
                    .sign(storage.getMinio().getEndpoint() + "/" + bucket + "/" + key);
            Path pythonRoot = Path.of("..", "ai-service").toAbsolutePath().normalize();
            String code = """
                    import asyncio, sys
                    from app.core.config import Settings
                    from app.services.image_analysis import ImageAnalysisService, validate_image_url, prepare_image
                    try:
                        settings = Settings(api_key='test-' + 'a' * 32, deepseek_api_key='', _env_file=None)
                        url = sys.stdin.read()
                        validate_image_url(url, settings)
                        content = asyncio.run(ImageAnalysisService(settings).download(url))
                        assert prepare_image(content).startswith('data:image/jpeg;base64,')
                    except Exception:
                        sys.exit(1)
                    """;
            Process process = new ProcessBuilder(pythonRoot.resolve(".venv/Scripts/python.exe").toString(), "-c", code)
                    .directory(pythonRoot.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            try {
                process.getOutputStream().write(signed.getBytes(StandardCharsets.UTF_8));
                process.getOutputStream().close();
                assertTrue(process.waitFor(35, TimeUnit.SECONDS), "Python download timed out");
                assertEquals(0, process.exitValue(), "Python signed download / image preparation failed");
            } finally {
                if (process.isAlive()) process.destroyForcibly();
            }
        } finally {
            minio.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        }
    }
}
