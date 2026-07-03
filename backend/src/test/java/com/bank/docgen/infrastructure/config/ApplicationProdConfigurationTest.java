package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class ApplicationProdConfigurationTest {

    @Test
    void prodProfileDisablesSwaggerEndpoints() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application-prod", new ClassPathResource("application-prod.yml"));

        assertThat(sources).isNotEmpty();
        PropertySource<?> source = sources.getFirst();
        assertThat(source.getProperty("springdoc.api-docs.enabled")).isEqualTo(Boolean.FALSE);
        assertThat(source.getProperty("springdoc.swagger-ui.enabled")).isEqualTo(Boolean.FALSE);
    }
}
