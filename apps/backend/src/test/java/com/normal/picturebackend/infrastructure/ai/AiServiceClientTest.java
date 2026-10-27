package com.normal.picturebackend.infrastructure.ai;

import com.normal.picturebackend.infrastructure.config.AiServiceProperties;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AiServiceClientTest {
    private static final String KEY = "test-key-abcdefghijklmnopqrstuvwxyz012345";
    private static final String BODY = """
            {"code":0,"message":"ok","data":{"name":"smart-picture-ai-service",
            "version":"0.1.0","capabilities":[]},"request_id":"java-test-001"}
            """;
    private AiServiceProperties properties;
    private MockRestServiceServer server;
    private AiServiceClient client;

    @BeforeEach
    void setUp() {
        properties = new AiServiceProperties();
        properties.setApiKey(KEY);
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AiServiceClient(builder.build(), properties);
    }

    @Test
    void sendsAuthenticationAndTraceHeadersAndParsesPythonResponse() {
        server.expect(requestTo("http://127.0.0.1:8334/api/v1/info"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("X-API-Key", KEY))
                .andExpect(header("X-Request-ID", "java-test-001"))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));
        AiServiceInfo info = client.getInfo("java-test-001");
        assertEquals("smart-picture-ai-service", info.name());
        assertTrue(info.capabilities().isEmpty());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {301, 401, 403, 500, 503})
    void mapsHttpFailureWithoutExposingUpstreamBody(int status) {
        server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status))
                .body("private-provider-message").contentType(MediaType.TEXT_PLAIN));
        BusinessException exception = assertThrows(BusinessException.class,
                () -> client.getInfo("java-test-001"));
        assertEquals(50001, exception.getCode());
        assertFalse(exception.getMessage().contains("private-provider-message"));
        assertFalse(exception.getMessage().contains(KEY));
        assertTrue(exception.getMessage().contains("java-test-001"));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-json", "{}", "{\"code\":0,\"data\":null}",
            "{\"code\":422,\"message\":\"private\"}"})
    void rejectsMalformedOrFailedResponses(String body) {
        server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThrows(BusinessException.class, () -> client.getInfo("java-test-001"));
        server.verify();
    }

    @Test
    void rejectsMismatchedTraceId() {
        server.expect(anything()).andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));
        assertThrows(BusinessException.class, () -> client.getInfo("different-id"));
    }

    @Test
    void mapsTimeout() {
        server.expect(anything()).andRespond(withException(new SocketTimeoutException("private")));
        BusinessException exception = assertThrows(BusinessException.class, client::getInfo);
        assertTrue(exception.getMessage().contains("超时"));
        assertFalse(exception.getMessage().contains("private"));
    }

    @Test
    void missingKeyDoesNotMakeNetworkRequest() {
        properties.setApiKey("");
        assertThrows(BusinessException.class, client::getInfo);
        server.verify();
    }

    @Test
    void normalizesUnsafeRequestIds() {
        assertEquals("trace-001", AiServiceClient.normalizeRequestId("trace-001"));
        for (String value : new String[]{null, "", "x".repeat(65), "bad\r\nheader"}) {
            assertTrue(AiServiceClient.normalizeRequestId(value).matches("[A-Za-z0-9._-]{1,64}"));
        }
    }

    @Test
    void postsSignedUrlAndParsesAnalysis() {
        server.expect(requestTo("http://127.0.0.1:8334/api/v1/images/analyze"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("X-API-Key", KEY))
                .andExpect(content().json("{\"image_url\":\"signed-url\"}"))
                .andRespond(withSuccess("""
                        {"code":0,"data":{"title":"海边","tags":["风景"]},"request_id":"java-test-001"}
                        """, MediaType.APPLICATION_JSON));
        assertEquals("海边", client.analyzeImage("signed-url", "java-test-001").title());
        server.verify();
    }

    @Test
    void rejectsInvalidAnalysisMetadata() {
        server.expect(anything()).andRespond(withSuccess("""
                {"code":0,"data":{"title":"标题","tags":[]},"request_id":"java-test-001"}
                """, MediaType.APPLICATION_JSON));
        assertThrows(BusinessException.class, () -> client.analyzeImage("signed-url", "java-test-001"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://localhost", "http://user:password@localhost",
            "http://localhost?key=secret", "http://localhost/path", "invalid"})
    void rejectsUnsafeBaseUrls(String url) {
        properties.setBaseUrl(url);
        assertFalse(properties.isValidBaseUrl());
    }
}
