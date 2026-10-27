package com.normal.picturebackend.infrastructure.ai;

import java.util.List;

/** Python 服务公开的能力信息，不包含运行配置或密钥。 */
public record AiServiceInfo(String name, String version, List<String> capabilities) {
}
