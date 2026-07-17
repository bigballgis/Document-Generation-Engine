package com.bank.docgen.runtime.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.apimgmt.service.AdGroupResolver;
import com.bank.docgen.apimgmt.service.ConfigAdGroupResolver;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * BDD-PRR-B02-LDAP-SPI-001 — filter depends on AdGroupResolver SPI, not ConfigAdGroupResolver.
 */
class ApiCredentialAuthenticationFilterSpiDependencyTest {

    @Test
    void fieldTypeIsAdGroupResolverInterface() throws Exception {
        Field field = ApiCredentialAuthenticationFilter.class.getDeclaredField("adGroupResolver");
        assertThat(field.getType()).isEqualTo(AdGroupResolver.class);
        assertThat(field.getType().isInterface()).isTrue();
        assertThat(field.getType()).isNotEqualTo(ConfigAdGroupResolver.class);
    }

    @Test
    void constructorParameterIsAdGroupResolverInterface() {
        Constructor<?>[] constructors = ApiCredentialAuthenticationFilter.class.getConstructors();
        assertThat(constructors).hasSize(1);
        Parameter[] parameters = constructors[0].getParameters();
        assertThat(Arrays.stream(parameters).map(Parameter::getType))
                .contains(AdGroupResolver.class)
                .doesNotContain(ConfigAdGroupResolver.class);
    }
}
