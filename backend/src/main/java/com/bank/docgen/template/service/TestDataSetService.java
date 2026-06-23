package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TestDataSetView;
import com.bank.docgen.template.api.UpsertTestDataSetRequest;
import com.bank.docgen.template.persistence.TestDataSetEntity;
import com.bank.docgen.template.persistence.TestDataSetRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestDataSetService {

    private final TemplateService templateService;
    private final TestDataSetRepository testDataSetRepository;
    private final ObjectMapper objectMapper;

    public TestDataSetService(
            TemplateService templateService,
            TestDataSetRepository testDataSetRepository,
            ObjectMapper objectMapper
    ) {
        this.templateService = templateService;
        this.testDataSetRepository = testDataSetRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TestDataSetView> list(UUID templateId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        return testDataSetRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public TestDataSetView get(UUID templateId, String externalId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        return toView(requireDataSet(templateId, externalId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resolveVariables(UUID templateId, String externalId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        TestDataSetEntity entity = requireDataSet(templateId, externalId);
        return readVariables(entity.getVariablesJson());
    }

    @Transactional
    public TestDataSetView create(UUID templateId, UpsertTestDataSetRequest request, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        String externalId = "TDS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        TestDataSetEntity entity = new TestDataSetEntity(
                UUID.randomUUID(),
                templateId,
                externalId,
                request.name(),
                request.description(),
                writeVariables(request.variables())
        );
        return toView(testDataSetRepository.save(entity));
    }

    @Transactional
    public TestDataSetView update(
            UUID templateId,
            String externalId,
            UpsertTestDataSetRequest request,
            ManagementSessionClaims session
    ) {
        templateService.requireReadableTemplate(templateId, session);
        TestDataSetEntity entity = requireDataSet(templateId, externalId);
        entity.update(request.name(), request.description(), writeVariables(request.variables()));
        return toView(testDataSetRepository.save(entity));
    }

    @Transactional
    public void delete(UUID templateId, String externalId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        TestDataSetEntity entity = requireDataSet(templateId, externalId);
        testDataSetRepository.delete(entity);
    }

    private TestDataSetEntity requireDataSet(UUID templateId, String externalId) {
        return testDataSetRepository.findByTemplateIdAndExternalId(templateId, externalId)
                .orElseThrow(TestDataSetNotFoundException::new);
    }

    private TestDataSetView toView(TestDataSetEntity entity) {
        return new TestDataSetView(
                entity.getExternalId(),
                entity.getTemplateId().toString(),
                entity.getName(),
                entity.getDescription(),
                readVariables(entity.getVariablesJson()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String writeVariables(Map<String, Object> variables) {
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    private Map<String, Object> readVariables(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }
}
