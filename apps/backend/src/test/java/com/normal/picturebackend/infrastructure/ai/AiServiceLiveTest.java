package com.normal.picturebackend.infrastructure.ai;

import com.normal.picturebackend.infrastructure.config.AiServiceConfig;
import com.normal.picturebackend.infrastructure.config.AiServiceProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

/** 显式启用时调用真实 Python；不加载数据库、Redis、存储或业务 Web 服务。 */
@EnabledIfEnvironmentVariable(named = "AI_SERVICE_LIVE_TEST", matches = "true")
@SpringBootTest(classes = AiServiceLiveTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.profiles.active=ai-smoke")
class AiServiceLiveTest {
    @Autowired
    private AiServiceClient client;

    @Test
    void callsRealPythonService() {
        AiServiceInfo info = client.getInfo("springboot-live-info-001");
        assertEquals("smart-picture-ai-service", info.name());
        assertNotNull(info.version());
        assertNotNull(info.capabilities());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiServiceProperties.class)
    @Import({AiServiceConfig.class, AiServiceClient.class})
    @ImportAutoConfiguration({JacksonAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class, RestClientAutoConfiguration.class})
    static class Config {
    }
}
