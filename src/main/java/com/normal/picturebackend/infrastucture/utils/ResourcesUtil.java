package com.normal.picturebackend.infrastucture.utils;


import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Component
@Slf4j
public class ResourcesUtil {


    @Resource
    private ResourceLoader resourceLoader;

    /**
     * 获取静态资源文件
     * 注意: 使用完毕记得手动删除
     * 调用File.delete方法;
     *
     * @param path 静态资源路径 如 (static/....)
     * @return File
     */
    public File getResourceFile(String path) throws IOException {

        // 获取资源文件
        org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:" + path);
        if (!resource.exists()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "静态资源不存在");
        }

        try (InputStream inputStream = resource.getInputStream()) {
            // 创建临时文件
            File tempFile = File.createTempFile("normal", ".test");

            // 标记JVM退出时删除临时文件 （安全网） - 注意: JVM崩溃或强制终止不会删除
            tempFile.deleteOnExit();

            // 将静态文件写入临时文件中
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            log.debug("创建临时文件: {}", tempFile.getAbsolutePath());

            // 返回读取的静态文件
            return tempFile;
        }
    }
}
