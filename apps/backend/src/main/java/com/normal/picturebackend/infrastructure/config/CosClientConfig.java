package com.normal.picturebackend.infrastructure.config;

import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cos对象存储配置
 *
 * @author leikooo
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {  
  
    /**  
     * 域名  
     */  
    private String host;  
  
    /**  
     * secretId  
     */  
    private String secretId;  
  
    /**  
     * 密钥（注意不要泄露）  
     */  
    private String secretKey;  
  
    /**  
     * 区域  
     */  
    private String region;  
  
    /**  
     * 桶名  
     */  
    private String bucket;  
  
    @Bean
    public COSClient cosClient() {
        // COS 配置缺失时也保证应用可以启动（例如只使用本地 MinIO 的场景）
        // 此时 COS 实现会在 isConfigured() 中判断为不可用，不会被切换选中
        if (StrUtil.isBlank(secretId) || StrUtil.isBlank(secretKey)) {
            log.warn("COS 配置不完整（secretId/secretKey 为空），COS 对象存储将不可用；如需使用 COS 请补全 cos.client 配置");
        }
        String accessKey = StrUtil.blankToDefault(secretId, "cos-not-configured");
        String accessSecret = StrUtil.blankToDefault(secretKey, "cos-not-configured");
        String regionName = StrUtil.blankToDefault(region, "ap-guangzhou");

        // 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials(accessKey, accessSecret);
        // 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region(regionName));
        // 这里建议设置使用 https 协议
        // 从 5.6.54 版本开始，默认使用了 https
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 生成cos客户端
        return new COSClient(cred, clientConfig);
    }
}

