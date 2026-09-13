package com.bravetest.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局配置：Long 序列化为字符串。
 * <p>雪花 id 为 19 位 Long，超出 JavaScript Number.MAX_SAFE_INTEGER（2^53-1，16 位），
 * 浏览器 JSON.parse 会丢失精度（如 2098758808064270338 → 2098758808064270300），
 * 导致前端拿错误 id 调购买/借阅等接口时后端查无记录，报"信息集不存在或已下架"。
 * 统一序列化为字符串后前端原样回传，Spring 可自动转换为 Long。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}
