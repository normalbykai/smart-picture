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
import java.util.Map;

@Slf4j
@Component
public class AiServiceClient {
    private static final ParameterizedTypeReference<AiServiceResponse<AiServiceInfo>> INFO_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<AiServiceResponse<AiImageAnalysisResult>> ANALYSIS_TYPE =
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
        AiServiceInfo info = execute(restClient.get().uri("/api/v1/info"), INFO_TYPE, requestId);
        if (info.name() == null || info.name().isBlank() || info.version() == null || info.version().isBlank()
                || info.capabilities() == null || info.capabilities().stream().anyMatch(v -> v == null)) {
            throw failure("AI 服务响应格式不正确", requestId);
        }
        return info;
    }

    public AiImageAnalysisResult analyzeImage(String signedUrl, String suppliedRequestId) {
        String requestId = normalizeRequestId(suppliedRequestId);
        AiImageAnalysisResult result = execute(restClient.post().uri("/api/v1/images/analyze")
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("image_url", signedUrl)),
                ANALYSIS_TYPE, requestId);
        if (!result.isValid()) {
            throw failure("AI 服务返回的标题或标签不符合要求", requestId);
        }
        return result;
    }

    private <T> T execute(RestClient.RequestHeadersSpec<?> request,
            ParameterizedTypeReference<AiServiceResponse<T>> type, String requestId) {
        String apiKey = properties.getApiKey();
        if (apiKey == null || !apiKey.matches("[\\x21-\\x7E]{32,}")) {
            throw failure("AI 服务密钥未配置或格式不正确", requestId);
        }
        try {
            AiServiceResponse<T> result = request
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", apiKey)
                    .header("X-Request-ID", requestId)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (httpRequest, response) -> {
                        int status = response.getStatusCode().value();
                        log.warn("AI service HTTP failure status={} request_id={}", status, requestId);
                        String message = switch (status) {
                            case 401, 403 -> "AI 服务认证失败";
                            case 413, 422 -> "图片格式、大小或存储地址不符合分析要求";
                            case 429 -> "AI 分析繁忙或调用频率受限，请稍后再试";
                            case 503 -> "AI 服务未就绪，请检查模型密钥配置";
                            case 504 -> "AI 图片分析超时，请稍后再试";
                            default -> "AI 服务暂不可用";
                        };
                        throw failure(message, requestId);
                    })
                    .body(type);
            if (result == null || !Integer.valueOf(0).equals(result.code())) {
                throw failure("AI 服务返回失败结果", requestId);
            }
            if (result.data() == null || !requestId.equals(result.requestId())) {
                throw failure("AI 服务响应格式不正确", requestId);
            }
            log.info("AI service request success request_id={}", requestId);
            return result.data();
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
