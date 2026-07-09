package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * F4-A2: {@link DocgenRenderingProperties} and {@link PdfConversionExecutorConfig} binding evidence.
 */
class DocgenRenderingPropertiesBindingTest {

    @Test
    void applicationYamlExposesRenderingPoolTimeoutAndPaginationBudget() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("docgen.rendering.conversion-pool-size"))
                .isEqualTo("${PDF_CONVERSION_POOL_SIZE:2}");
        assertThat(properties.getProperty("docgen.rendering.conversion-timeout-seconds"))
                .isEqualTo("${PDF_CONVERSION_TIMEOUT_SECONDS:120}");
        assertThat(properties.getProperty("docgen.rendering.conversion-queue-capacity"))
                .isEqualTo("${PDF_CONVERSION_QUEUE_CAPACITY:0}");
        assertThat(properties.getProperty("docgen.rendering.pagination-delta-budget-pages"))
                .isEqualTo("${PAGINATION_DELTA_BUDGET_PAGES:1}");
    }

    @Test
    void bindsConversionPoolSizeFromProperties() {
        DocgenRenderingProperties properties = bind(Map.of(
                "docgen.rendering.conversion-pool-size", "4"
        ));

        assertThat(properties.getConversionPoolSize()).isEqualTo(4);
    }

    @Test
    void bindsPaginationDeltaBudgetPagesFromProperties() {
        DocgenRenderingProperties properties = bind(Map.of(
                "docgen.rendering.pagination-delta-budget-pages", "2"
        ));

        assertThat(properties.getPaginationDeltaBudgetPages()).isEqualTo(2);
    }

    @Test
    void pdfConversionExecutorReflectsConfiguredPoolSize() {
        DocgenRenderingProperties renderingProperties = new DocgenRenderingProperties();
        renderingProperties.setConversionPoolSize(4);
        renderingProperties.setConversionQueueCapacity(0);

        ThreadPoolTaskExecutor executor =
                new PdfConversionExecutorConfig().pdfConversionExecutor(renderingProperties);
        try {
            assertThat(executor.getCorePoolSize()).isEqualTo(4);
            assertThat(executor.getMaxPoolSize()).isEqualTo(4);
            assertThat(ReflectionTestUtils.getField(executor, "queueCapacity")).isEqualTo(0);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void renderingPropertiesDefaultsMatchCorP02AndSorP03() {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();

        assertThat(properties.getConversionPoolSize()).isEqualTo(2);
        assertThat(properties.getConversionTimeoutSeconds()).isEqualTo(120);
        assertThat(properties.getConversionQueueCapacity()).isEqualTo(0);
        assertThat(properties.getPaginationDeltaBudgetPages()).isEqualTo(1);
    }

    private static DocgenRenderingProperties bind(Map<String, String> source) {
        DocgenRenderingProperties bound = new Binder(new MapConfigurationPropertySource(source))
                .bind("docgen.rendering", Bindable.of(DocgenRenderingProperties.class))
                .orElse(null);
        assertThat(bound).isNotNull();
        return bound;
    }
}
