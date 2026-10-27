package com.normal.picturebackend.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 独立于 Java 业务错误码的 Python HTTP 协议。 */
public record AiServiceResponse<T>(Integer code, String message, T data,
                                   @JsonProperty("request_id") String requestId) {
}
