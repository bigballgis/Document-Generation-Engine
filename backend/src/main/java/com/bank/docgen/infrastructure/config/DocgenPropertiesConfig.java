package com.bank.docgen.infrastructure.config;

import com.bank.docgen.apimgmt.service.AdGroupResolverProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AdGroupResolverProperties.class,
        DocgenIdempotencyProperties.class,
        DocgenAsyncProperties.class,
        DocgenRenderingProperties.class,
        RuntimeRateLimitProperties.class
})
public class DocgenPropertiesConfig {
}
