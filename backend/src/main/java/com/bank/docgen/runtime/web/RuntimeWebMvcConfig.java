package com.bank.docgen.runtime.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CE-C02: registers the runtime-only strict JSON converter at the front of the message-converter
 * chain so that {@link com.bank.docgen.runtime.api.GenerateRequestBody} and
 * {@link com.bank.docgen.runtime.api.BatchGenerateRequestBody} are deserialized with
 * fail-on-unknown-properties, while every other (management / rendering) request body keeps the
 * default lax behavior.
 */
@Configuration
public class RuntimeWebMvcConfig implements WebMvcConfigurer {

    private final ObjectMapper sharedObjectMapper;

    public RuntimeWebMvcConfig(ObjectMapper sharedObjectMapper) {
        this.sharedObjectMapper = sharedObjectMapper;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, new RuntimeStrictJacksonHttpMessageConverter(sharedObjectMapper));
    }
}
