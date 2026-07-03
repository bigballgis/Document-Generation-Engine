package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiPolicyMaterializationService {

    private final ApiPolicyRepository apiPolicyRepository;

    public ApiPolicyMaterializationService(ApiPolicyRepository apiPolicyRepository) {
        this.apiPolicyRepository = apiPolicyRepository;
    }

    @Transactional
    public ApiPolicyEntity ensureApiPolicySkeleton(UUID templateId, String actor) {
        return apiPolicyRepository.findByTemplateId(templateId)
                .orElseGet(() -> apiPolicyRepository.save(ApiPolicyEntity.createSkeleton(templateId, actor)));
    }

    @Transactional
    public ApiPolicyEntity ensureApiPolicyOnPublish(UUID templateId, String releaseVersion, String actor) {
        ApiPolicyEntity policy = ensureApiPolicySkeleton(templateId, actor);
        policy.materializeDefaultRouteOnFirstPublish(releaseVersion, actor);
        return apiPolicyRepository.save(policy);
    }
}
