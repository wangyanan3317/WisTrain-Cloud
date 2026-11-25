package com.wistrain.cloud.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 日期格式兼容（US-201）：前端日期控件输出 "yyyy-MM-dd HH:mm:ss"（空格分隔），
 * 而 LocalDateTime 默认只认 ISO "yyyy-MM-dd'T'HH:mm:ss"，此处统一兼容三种格式。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter SPACE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> builder.deserializerByType(LocalDateTime.class, new JsonDeserializer<>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String text = p.getText() == null ? "" : p.getText().trim();
                if (text.isEmpty()) {
                    return null;
                }
                try {                                       // ISO：2026-11-05T00:00:00
                    return LocalDateTime.parse(text);
                } catch (DateTimeException ignored) { }
                try {                                       // 空格：2026-11-05 00:00:00
                    return LocalDateTime.parse(text, SPACE_FMT);
                } catch (DateTimeException ignored) { }
                try {                                       // 仅日期：2026-11-05 → 当日零点
                    return LocalDate.parse(text, DATE_FMT).atStartOfDay();
                } catch (DateTimeException ignored) { }
                throw new IllegalArgumentException("日期格式不正确，应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
            }
        });
    }

    /** 供需要显式反序列化器（如 @JsonFormat 场景）复用 */
    @Bean
    public LocalDateTimeDeserializer spaceFmtDeserializer() {
        return new LocalDateTimeDeserializer(SPACE_FMT);
    }
}
