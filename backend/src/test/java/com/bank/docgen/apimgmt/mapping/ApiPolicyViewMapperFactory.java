package com.bank.docgen.apimgmt.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class ApiPolicyViewMapperFactory {

    private ApiPolicyViewMapperFactory() {
    }

    public static ApiPolicyViewMapper create(ObjectMapper objectMapper) {
        return new ApiPolicyViewMapperImpl(new ApiPolicyMappingSupport(objectMapper));
    }
}
