package com.bank.docgen.template.service;

import com.bank.docgen.template.api.CoverageThresholdView;
import com.bank.docgen.template.domain.CoverageThresholdScope;
import com.bank.docgen.template.persistence.CoverageThresholdConfigEntity;
import com.bank.docgen.template.persistence.CoverageThresholdConfigRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import org.springframework.stereotype.Service;

@Service
public class CoverageThresholdResolver {

    private static final int DEFAULT_MIN_REQUIRED_VARIABLE_PCT = 80;
    private static final int DEFAULT_MIN_REQUIRED_SAMPLE_PCT = 100;
    private static final int DEFAULT_MIN_ANCHOR_BINDING_PCT = 80;

    private final CoverageThresholdConfigRepository coverageThresholdConfigRepository;

    public CoverageThresholdResolver(CoverageThresholdConfigRepository coverageThresholdConfigRepository) {
        this.coverageThresholdConfigRepository = coverageThresholdConfigRepository;
    }

    public CoverageThresholdView resolveForTemplate(TemplateEntity template) {
        return coverageThresholdConfigRepository
                .findByScopeTypeAndGroupCode(CoverageThresholdScope.GROUP, template.getGroupCode())
                .map(this::toView)
                .orElseGet(this::globalOrDefault);
    }

    private CoverageThresholdView globalOrDefault() {
        return coverageThresholdConfigRepository
                .findByScopeTypeAndGroupCode(CoverageThresholdScope.GLOBAL, null)
                .map(this::toView)
                .orElse(new CoverageThresholdView(
                        CoverageThresholdScope.GLOBAL.name(),
                        null,
                        DEFAULT_MIN_REQUIRED_VARIABLE_PCT,
                        DEFAULT_MIN_REQUIRED_SAMPLE_PCT,
                        DEFAULT_MIN_ANCHOR_BINDING_PCT
                ));
    }

    private CoverageThresholdView toView(CoverageThresholdConfigEntity entity) {
        return new CoverageThresholdView(
                entity.getScopeType().name(),
                entity.getGroupCode(),
                entity.getMinRequiredVariablePct(),
                entity.getMinRequiredSamplePct(),
                entity.getMinAnchorBindingPct()
        );
    }
}
