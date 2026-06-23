package com.bank.docgen.template.service;

import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.template.api.TemplateRuleValidationItemRequest;
import com.bank.docgen.template.api.TemplateRuleValidationItemResponse;
import com.bank.docgen.template.api.TemplateRuleValidationRequest;
import com.bank.docgen.template.api.TemplateRuleValidationSummaryView;
import com.bank.docgen.template.api.TemplateRuleValidationView;
import com.bank.docgen.template.domain.RuleValidationStatus;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateRuleValidationService {

    private static final Pattern VARIABLE_REFERENCE = Pattern.compile("\\$\\{([A-Za-z0-9_]+)\\}");

    private final TemplateService templateService;
    private final TemplateVersionRepository templateVersionRepository;
    private final VariableSchemaRepository variableSchemaRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final MasterDocumentRepository masterDocumentRepository;

    public TemplateRuleValidationService(
            TemplateService templateService,
            TemplateVersionRepository templateVersionRepository,
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            MasterDocumentRepository masterDocumentRepository
    ) {
        this.templateService = templateService;
        this.templateVersionRepository = templateVersionRepository;
        this.variableSchemaRepository = variableSchemaRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.masterDocumentRepository = masterDocumentRepository;
    }

    @Transactional(readOnly = true)
    public TemplateRuleValidationView validateRules(
            java.util.UUID templateId,
            TemplateRuleValidationRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = currentDevVersion(templateId);
        Set<String> variableKeys = loadVariableKeys(version.getId());
        Set<String> anchorIds = loadAnchorIds(template, version.getId());
        Set<String> ruleIds = request.rules().stream()
                .map(TemplateRuleValidationItemRequest::ruleId)
                .collect(java.util.stream.Collectors.toSet());

        List<TemplateRuleValidationItemResponse> responses = new ArrayList<>();
        int valid = 0;
        int missingVariable = 0;
        int missingAnchor = 0;
        int invalidBranch = 0;
        int malformed = 0;

        for (TemplateRuleValidationItemRequest rule : request.rules()) {
            RuleValidationStatus status = validateRule(rule, variableKeys, anchorIds, ruleIds);
            responses.add(new TemplateRuleValidationItemResponse(
                    rule.ruleId(),
                    rule.conditionExpression(),
                    rule.targetAnchorId(),
                    rule.trueBranchRuleId(),
                    rule.falseBranchRuleId(),
                    status
            ));
            switch (status) {
                case VALID -> valid++;
                case MISSING_VARIABLE -> missingVariable++;
                case MISSING_ANCHOR -> missingAnchor++;
                case INVALID_BRANCH_REFERENCE -> invalidBranch++;
                case MALFORMED_RULE -> malformed++;
            }
        }

        boolean blocking = missingVariable > 0 || missingAnchor > 0 || invalidBranch > 0 || malformed > 0;
        TemplateRuleValidationSummaryView summary = new TemplateRuleValidationSummaryView(
                blocking,
                request.rules().size(),
                valid,
                missingVariable,
                missingAnchor,
                invalidBranch,
                malformed
        );
        return new TemplateRuleValidationView(!blocking, responses, summary);
    }

    private RuleValidationStatus validateRule(
            TemplateRuleValidationItemRequest rule,
            Set<String> variableKeys,
            Set<String> anchorIds,
            Set<String> ruleIds
    ) {
        if (isBlank(rule.ruleId()) || isBlank(rule.conditionExpression()) || isBlank(rule.targetAnchorId())) {
            return RuleValidationStatus.MALFORMED_RULE;
        }
        if (!anchorIds.contains(rule.targetAnchorId())) {
            return RuleValidationStatus.MISSING_ANCHOR;
        }
        Matcher matcher = VARIABLE_REFERENCE.matcher(rule.conditionExpression());
        while (matcher.find()) {
            if (!variableKeys.contains(matcher.group(1))) {
                return RuleValidationStatus.MISSING_VARIABLE;
            }
        }
        if (rule.trueBranchRuleId() != null && !ruleIds.contains(rule.trueBranchRuleId())) {
            return RuleValidationStatus.INVALID_BRANCH_REFERENCE;
        }
        if (rule.falseBranchRuleId() != null && !ruleIds.contains(rule.falseBranchRuleId())) {
            return RuleValidationStatus.INVALID_BRANCH_REFERENCE;
        }
        return RuleValidationStatus.VALID;
    }

    private Set<String> loadVariableKeys(java.util.UUID versionId) {
        Set<String> keys = new HashSet<>();
        for (VariableSchemaEntity variable : variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId)) {
            keys.add(variable.getVariableKey());
        }
        return keys;
    }

    private Set<String> loadAnchorIds(TemplateEntity template, java.util.UUID versionId) {
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(template.getMasterId())
                .orElseThrow(MasterNotFoundException::new);
        Set<String> anchorIds = new HashSet<>();
        master.getAnchors().forEach(anchor -> anchorIds.add(anchor.getAnchorId()));
        for (AnchorBindingEntity binding : anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId)) {
            anchorIds.add(binding.getAnchorId());
        }
        return anchorIds;
    }

    private TemplateVersionEntity currentDevVersion(java.util.UUID templateId) {
        return templateVersionRepository.findByTemplateIdAndDevVersionNumber(templateId, 1)
                .orElseThrow(TemplateNotFoundException::new);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
