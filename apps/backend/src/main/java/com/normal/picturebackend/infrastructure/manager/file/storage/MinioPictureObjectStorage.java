package com.normal.picturebackend.infrastructure.manager.file.storage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.normal.picturebackend.infrastructure.config.StorageProperties;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import com.normal.picturebackend.infrastructure.manager.file.model.UploadPictureResult;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 本地 MinIO 图片存储实现
 * <p>
 * MinIO 不提供数据万象之类的图片处理能力，因此：
 * <ul>
 *     <li>原图直接上传，{@code url} 与 {@code originUrl} 均指向原图；</li>
 *     <li>使用 JDK ImageIO 本地读取图片宽高、格式与主色调；</li>
 *     <li>图片大于 20KB 时在本地生成 512px 缩略图并上传。</li>
 * </ul>
 */
@Slf4j
@Component
public class MinioPictureObjectStorage implements PictureObjectStorage {

    /**
     * 采样计算主色调时的最大采样边长
     */
    private static final int COLOR_SAMPLE_SIZE = 100;

    private final MinioClient minioClient;

    private final StorageProperties storageProperties;

    private final Object bucketLock = new Object();

    private volatile boolean bucketReady = false;

    public MinioPictureObjectStorage(MinioClient minioClient, StorageProperties storageProperties) {
        this.minioClient = minioClient;
        this.storageProperties = storageProperties;
    }

    @Override
    public StorageTypeEnum getType() {
        return StorageTypeEnum.MINIO;
    }

    @Override
    public boolean isConfigured() {
        StorageProperties.Minio minio = minio();
        return StrUtil.isNotBlank(minio.getEndpoint())
                && StrUtil.isNotBlank(minio.getAccessKey())
                && StrUtil.isNotBlank(minio.getSecretKey())
                && StrUtil.isNotBlank(minio.getBucket());
    }

    @Override
    public boolean testConnection() {
        if (!isConfigured()) {
            return false;
        }
        try {
            ensureBucket();
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket()).build());
        } catch (Exception e) {
            log.warn("MinIO 连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public UploadPictureResult uploadPicture(String key, File file) {
        String objectKey = normalizeKey(key);
        String originUrl = uploadFile(objectKey, file);

        UploadPictureResult result = new UploadPictureResult();
        result.setUrl(originUrl);
        result.setOriginUrl(originUrl);
        result.setThumbnailUrl(originUrl);
        result.setPicSize(file.length());
        result.setPicFormat(resolveFormat(objectKey));

        BufferedImage image = readImage(file);
        if (image == null) {
            // ImageIO 不支持该格式（例如未安装 WebP 解码器）时，仍然保证原图可正常上传和使用
            log.warn("无法读取图片信息，跳过图片宽高、主色调和缩略图处理: key={}", objectKey);
            return result;
        }

        result.setPicWidth(image.getWidth());
        result.setPicHeight(image.getHeight());
        result.setPicScale(NumberUtil.round(image.getWidth() * 1.0 / image.getHeight(), 2).doubleValue());
        result.setMainColor(calculateMainColor(image));

        if (file.length() > minio().getThumbnailMinBytes()) {
            String thumbnailKey = generateAndUploadThumbnail(objectKey, image);
            if (StrUtil.isNotBlank(thumbnailKey)) {
                result.setThumbnailUrl(getUrl(thumbnailKey));
            }
        }
        return result;
    }

    @Override
    public String uploadFile(String key, File file) {
        String objectKey = normalizeKey(key);
        ensureBucket();
        try (InputStream inputStream = new FileInputStream(file)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket())
                    .object(objectKey)
                    .stream(inputStream, file.length(), -1)
                    .contentType(resolveContentType(objectKey))
                    .build());
        } catch (Exception e) {
            log.error("MinIO 上传文件失败: key={}", objectKey, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "MinIO 上传图片失败: " + e.getMessage());
        }
        return getUrl(objectKey);
    }

    @Override
    public void delete(String key) {
        String objectKey = normalizeKey(key);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("MinIO 删除文件失败: key={}", objectKey, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "MinIO 删除图片失败");
        }
    }

    @Override
    public String getUrl(String key) {
        String objectKey = normalizeKey(key);
        if (!minio().isPublicRead()) {
            // 私有桶：生成预签名地址（注意：数据库中保存的地址会在有效期后失效）
            try {
                int expiry = Math.min(Math.max(minio().getPresignedExpirySeconds(), 1), 7 * 24 * 60 * 60);
                return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket())
                        .object(objectKey)
                        .expiry(expiry, TimeUnit.SECONDS)
                        .build());
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成 MinIO 图片访问地址失败");
            }
        }
        return String.format("%s/%s/%s", minio().getEffectivePublicUrl(), bucket(), objectKey);
    }

    @Override
    public String getKey(String url) {
        if (StrUtil.isBlank(url)) {
            return "";
        }
        try {
            String path = URI.create(url).getPath();
            String publicPath = URI.create(minio().getEffectivePublicUrl()).getPath();
            if (StrUtil.isNotBlank(publicPath) && path.startsWith(publicPath)) {
                path = path.substring(publicPath.length());
            }
            path = StrUtil.removePrefix(path, "/");
            // 直接访问地址形如：/bucket/public/1/xxx.jpg
            String bucketPrefix = bucket() + "/";
            if (path.startsWith(bucketPrefix)) {
                path = path.substring(bucketPrefix.length());
            }
            return URLDecoder.decode(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("解析 MinIO 图片地址失败: {}", url);
            return StrUtil.removePrefix(url, "/");
        }
    }

    @Override
    public boolean matches(String url) {
        if (StrUtil.isBlank(url)) {
            return false;
        }
        String publicUrl = minio().getEffectivePublicUrl();
        String endpoint = StrUtil.removeSuffix(StrUtil.nullToEmpty(minio().getEndpoint()), "/");
        return (StrUtil.isNotBlank(publicUrl) && url.startsWith(publicUrl + "/"))
                || (StrUtil.isNotBlank(endpoint) && url.startsWith(endpoint + "/"));
    }

    /**
     * 确保桶存在，并在需要时设置公共读策略
     */
    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (bucketLock) {
            if (bucketReady) {
                return;
            }
            String bucket = bucket();
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    if (!minio().isAutoCreateBucket()) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "MinIO 桶不存在: " + bucket);
                    }
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("已自动创建 MinIO 桶: {}", bucket);
                }
                if (minio().isPublicRead()) {
                    applyPublicReadPolicy(bucket);
                }
                bucketReady = true;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("MinIO 初始化失败: bucket={}", bucket, e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "MinIO 初始化失败: " + e.getMessage());
            }
        }
    }

    private void applyPublicReadPolicy(String bucket) {
        String policy = String.format("""
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """, bucket);
        try {
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucket)
                    .config(policy)
                    .build());
        } catch (Exception e) {
            // 没有设置策略权限时不阻断上传，但图片可能无法通过 URL 直接访问
            log.warn("设置 MinIO 公共读策略失败，请手动将桶 {} 设置为公共读: {}", bucket, e.getMessage());
        }
    }

    /**
     * 生成本地缩略图并上传
     *
     * @return 缩略图对象键，未生成时返回 null
     */
    private String generateAndUploadThumbnail(String key, BufferedImage image) {
        int maxSize = Math.max(1, minio().getThumbnailMaxSize());
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= maxSize && height <= maxSize) {
            // 原图已经足够小，直接复用原图
            return null;
        }

        double ratio = Math.min(maxSize * 1.0 / width, maxSize * 1.0 / height);
        int targetWidth = Math.max(1, (int) Math.round(width * ratio));
        int targetHeight = Math.max(1, (int) Math.round(height * ratio));
        String format = resolveThumbnailFormat(key);
        BufferedImage thumbnail = scaleImage(image, targetWidth, targetHeight, format);

        File tempFile = null;
        try {
            tempFile = File.createTempFile("minio-thumbnail-", "." + format);
            if (!ImageIO.write(thumbnail, format, tempFile)) {
                log.warn("当前 JDK 不支持 {} 格式缩略图写出，跳过缩略图生成: key={}", format, key);
                return null;
            }
            String thumbnailKey = removeExtension(key) + "_thumbnail." + format;
            uploadFile(thumbnailKey, tempFile);
            return thumbnailKey;
        } catch (Exception e) {
            log.warn("生成 MinIO 缩略图失败，将使用原图地址: key={}, error={}", key, e.getMessage());
            return null;
        } finally {
            if (tempFile != null && !tempFile.delete()) {
                log.debug("缩略图临时文件删除失败: {}", tempFile.getAbsolutePath());
            }
        }
    }

    private BufferedImage scaleImage(BufferedImage source, int width, int height, String format) {
        boolean jpeg = "jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format);
        int imageType = jpeg || !source.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;
        BufferedImage target = new BufferedImage(width, height, imageType);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (imageType == BufferedImage.TYPE_INT_RGB) {
                // JPEG 不支持透明通道，透明区域填充白色
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
            }
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 采样计算平均色，格式与 COS 数据万象的 ave 保持一致：0xRRGGBB
     */
    private String calculateMainColor(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int step = Math.max(1, Math.max(width, height) / COLOR_SAMPLE_SIZE);
        long red = 0;
        long green = 0;
        long blue = 0;
        long count = 0;
        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha < 16) {
                    continue;
                }
                red += (argb >> 16) & 0xFF;
                green += (argb >> 8) & 0xFF;
                blue += argb & 0xFF;
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        return String.format("0x%02X%02X%02X", red / count, green / count, blue / count);
    }

    private BufferedImage readImage(File file) {
        try {
            return ImageIO.read(file);
        } catch (IOException | RuntimeException e) {
            log.warn("读取图片失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 缩略图优先使用原格式；JDK 不支持该格式写出时退回 jpg
     */
    private String resolveThumbnailFormat(String key) {
        String suffix = FileUtil.getSuffix(key);
        suffix = suffix == null ? "" : suffix.toLowerCase(Locale.ROOT);
        if (StrUtil.isNotBlank(suffix) && ImageIO.getImageWritersByFormatName(suffix).hasNext()) {
            return suffix;
        }
        return "jpg";
    }

    private String resolveFormat(String key) {
        String suffix = FileUtil.getSuffix(key);
        suffix = suffix == null ? "" : suffix.toLowerCase(Locale.ROOT);
        return StrUtil.blankToDefault(suffix, "unknown");
    }

    private String resolveContentType(String key) {
        return StrUtil.blankToDefault(FileUtil.getMimeType(key), "application/octet-stream");
    }

    private String normalizeKey(String key) {
        return StrUtil.strip(StrUtil.nullToEmpty(key), "/");
    }

    /**
     * 去掉对象键的扩展名，同时保留目录层级
     * 例如：public/1/a.jpg -> public/1/a
     */
    private String removeExtension(String key) {
        int slashIndex = key.lastIndexOf('/');
        int dotIndex = key.lastIndexOf('.');
        if (dotIndex > slashIndex) {
            return key.substring(0, dotIndex);
        }
        return key;
    }

    private String bucket() {
        return minio().getBucket();
    }

    private StorageProperties.Minio minio() {
        return storageProperties.getMinio();
    }
}
