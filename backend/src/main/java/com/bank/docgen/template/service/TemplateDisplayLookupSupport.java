package com.bank.docgen.template.service;

import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Package-private display-info lookup by template ids.
 */
final class TemplateDisplayLookupSupport {

    private final TemplateRepository templateRepository;

    TemplateDisplayLookupSupport(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    Map<UUID, TemplateService.TemplateDisplayInfo> lookupDisplayInfoByIds(Set<UUID> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> distinctIds = templateIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return templateRepository.findByIdInAndDeletedAtIsNull(distinctIds).stream()
                .collect(Collectors.toMap(
                        TemplateEntity::getId,
                        template -> new TemplateService.TemplateDisplayInfo(template.getName(), template.getExternalId())
                ));
    }
}
