package com.normal.picturebackend.infrastructure.ai;

import com.normal.picturebackend.infrastructure.config.AiServiceProperties;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Slf4j
@Component
public class AiServiceClient {
    private static final ParameterizedTypeReference<AiServiceResponse<AiServiceInfo>> INFO_TYPE =
            new ParameterizedTypeReference<>() {};
    private final RestClient restClient;
    private final AiServiceProperties properties;

    public AiServiceClient(@Qualifier("aiRestClient") RestClient restClient,
                           AiServiceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public AiServiceInfo getInfo() {
        return getInfo(null);
    }

    public AiServiceInfo getInfo(String suppliedRequestId) {
        String requestId = normalizeRequestId(suppliedRequestId);
        String apiKey = properties.getApiKey();
        if (apiKey == null || !apiKey.matches("[\\x21-\\x7E]{32,}")) {
            throw failure("AI 服务密钥未配置或格式不正确", requestId);
        }
        try {
            AiServiceResponse<AiServiceInfo> result = restClient.get()
                    .uri("/api/v1/info")
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", apiKey)
                    .header("X-Request-ID", requestId)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
                        int status = response.getStatusCode().value();
                        log.warn("AI service HTTP failure status={} request_id={}", status, requestId);
                        String message = status == 401 || status == 403
                                ? "AI 服务认证失败" : "AI 服务暂不可用";
                        throw failure(message, requestId);
                    })
                    .body(INFO_TYPE);
            if (result == null || !Integer.valueOf(0).equals(result.code())) {
                throw failure("AI 服务返回失败结果", requestId);
            }
            AiServiceInfo info = result.data();
            if (info == null || info.name() == null || info.name().isBlank()
                    || info.version() == null || info.version().isBlank()
                    || info.capabilities() == null || info.capabilities().stream().anyMatch(v -> v == null)
                    || !requestId.equals(result.requestId())) {
                throw failure("AI 服务响应格式不正确", requestId);
            }
            log.info("AI service info success request_id={}", requestId);
            return info;
        } catch (ResourceAccessException e) {
            // 不记录异常原文：厂商响应或连接信息可能含敏感数据。
            log.warn("AI service connection failure request_id={}", requestId);
            throw failure("AI 服务连接失败或请求超时", requestId);
        } catch (RestClientException e) {
            log.warn("AI service invalid response request_id={}", requestId);
            throw failure("AI 服务响应解析失败", requestId);
        }
    }

    public static String normalizeRequestId(String requestId) {
        return requestId != null && requestId.matches("[A-Za-z0-9._-]{1,64}")
                ? requestId : UUID.randomUUID().toString();
    }

    private BusinessException failure(String message, String requestId) {
        return new BusinessException(ErrorCode.OPERATION_ERROR, message + "（请求ID：" + requestId + "）");
    }
}
