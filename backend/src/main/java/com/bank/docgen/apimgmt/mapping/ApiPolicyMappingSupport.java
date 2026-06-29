package com.bank.docgen.apimgmt.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class ApiPolicyMappingSupport {

    private final ObjectMapper objectMapper;

    public ApiPolicyMappingSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Named("uuidToString")
    public String uuidToString(UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    @Named("readStringList")
    public List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    @Named("enumToName")
    public String enumToName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    @Named("runtimeFingerprint")
    public String runtimeFingerprint(String externalId) {
        return "fp-" + externalId;
    }
}
