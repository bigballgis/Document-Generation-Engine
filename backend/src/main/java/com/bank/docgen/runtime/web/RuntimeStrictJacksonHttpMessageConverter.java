package com.bank.docgen.runtime.web;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import java.lang.reflect.Type;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * CE-C02: runtime-only strict JSON deserialization for the public generation API request bodies.
 * <p>
 * Scope is intentionally narrow: only the runtime {@code @RequestBody} root types
 * ({@link GenerateRequestBody} and {@link BatchGenerateRequestBody}) are read by this converter,
 * which uses a copy of the shared {@link ObjectMapper} with
 * {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} enabled. Nested runtime objects
 * ({@code context}, {@code output}, {@code encryption}, batch {@code items[]} elements) inherit
 * strictness because they are deserialized by the same mapper instance.
 * <p>
 * Management / rendering request DTOs are <strong>not</strong> claimed by this converter
 * ({@link #canRead} returns {@code false} for them), so they continue to be handled by the
 * default lax {@link MappingJackson2HttpMessageConverter}. This keeps CE-C02 strictness isolated
 * to the runtime contract and preserves management DTO behavior (BDD-CE-C02-006).
 */
public class RuntimeStrictJacksonHttpMessageConverter extends MappingJackson2HttpMessageConverter {

    public RuntimeStrictJacksonHttpMessageConverter(ObjectMapper sharedMapper) {
        super(createStrictMapper(sharedMapper));
    }

    /**
     * Shared factory for HTTP converter and unit tests: unknown properties fail, and scalar
     * coercions into {@code String} (e.g. {@code context.locale: 123}) also fail (BDD-CE-C01-005).
     */
    public static ObjectMapper createStrictMapper(ObjectMapper sharedMapper) {
        ObjectMapper strict = sharedMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        strict.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Array, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Object, CoercionAction.Fail);
        return strict;
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return isRuntimeRequestBodyRoot(clazz) && super.canRead(clazz, mediaType);
    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
        if (type instanceof Class<?> clazz && isRuntimeRequestBodyRoot(clazz)) {
            return super.canRead(type, contextClass, mediaType);
        }
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        // Read-only converter: responses stay on the default Jackson converter chain.
        return false;
    }

    @Override
    public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
        return false;
    }

    private static boolean isRuntimeRequestBodyRoot(Class<?> clazz) {
        return GenerateRequestBody.class.equals(clazz)
                || BatchGenerateRequestBody.class.equals(clazz);
    }
}
