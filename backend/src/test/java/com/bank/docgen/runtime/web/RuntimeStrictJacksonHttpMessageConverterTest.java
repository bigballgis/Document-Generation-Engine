package com.bank.docgen.runtime.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

class RuntimeStrictJacksonHttpMessageConverterTest {

    private RuntimeStrictJacksonHttpMessageConverter strictConverter;
    private MappingJackson2HttpMessageConverter defaultLaxConverter;

    @BeforeEach
    void setUp() {
        ObjectMapper shared = new ObjectMapper().disable(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        strictConverter = new RuntimeStrictJacksonHttpMessageConverter(shared);
        defaultLaxConverter = new MappingJackson2HttpMessageConverter(shared);
    }

    @Test
    void canReadClaimsRuntimeGenerateRequestBodyRoot() {
        assertThat(strictConverter.canRead(GenerateRequestBody.class, MediaType.APPLICATION_JSON)).isTrue();
    }

    @Test
    void canReadClaimsRuntimeBatchGenerateRequestBodyRoot() {
        assertThat(strictConverter.canRead(BatchGenerateRequestBody.class, MediaType.APPLICATION_JSON)).isTrue();
    }

    @Test
    void canReadDoesNotClaimManagementDtoIsolatingStrictnessBddCeC02_006() {
        // A management DTO (e.g. a simple login-shaped record) must NOT be claimed by the strict
        // converter; it must fall through to the default lax converter that ignores unknown fields.
        Class<?> managementDto = ManagementLoginSampleDto.class;
        assertThat(strictConverter.canRead(managementDto, MediaType.APPLICATION_JSON)).isFalse();
        assertThat(defaultLaxConverter.canRead(managementDto, MediaType.APPLICATION_JSON)).isTrue();
    }

    @Test
    void canReadRejectsNonJsonMediaTypeForRuntimeRoots() {
        assertThat(strictConverter.canRead(GenerateRequestBody.class, MediaType.TEXT_PLAIN)).isFalse();
    }

    @Test
    void canWriteNeverClaimsSoDefaultConverterOwnsResponses() {
        assertThat(strictConverter.canWrite(GenerateRequestBody.class, MediaType.APPLICATION_JSON)).isFalse();
        assertThat(strictConverter.canWrite(Object.class, MediaType.APPLICATION_JSON)).isFalse();
    }

    /** Stand-in for an arbitrary management write DTO — must stay lax (CE-C02-006). */
    public record ManagementLoginSampleDto(String username, String password) {
    }
}
