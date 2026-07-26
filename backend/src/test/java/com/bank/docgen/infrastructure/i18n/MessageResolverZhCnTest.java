package com.bank.docgen.infrastructure.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

class MessageResolverZhCnTest {

    private MessageResolver messageResolver;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setDefaultLocale(Locale.ENGLISH);
        messageResolver = new MessageResolver(source);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void resolvesCollaborationSummaryInZhCn() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        String text = messageResolver.resolve("api.collaboration.workItem.submitForTest.summary");
        assertThat(text).contains("测试");
        assertThat(text).doesNotContain("Template submitted for testing");
    }

    @Test
    void fallsBackToEnglishWhenLocaleIsEnglish() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        String text = messageResolver.resolve("api.collaboration.workItem.submitForTest.summary");
        assertThat(text).isEqualTo("Template submitted for testing");
    }
}
