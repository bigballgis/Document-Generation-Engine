package com.bank.docgen.rendering.service;

import com.bank.docgen.rendering.api.FidelityWarningView;
import com.bank.docgen.rendering.api.PreviewComparisonItemView;
import com.bank.docgen.rendering.api.PreviewComparisonView;
import com.bank.docgen.rendering.domain.FidelityWarningCode;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.domain.PreviewComparisonLocationType;
import com.bank.docgen.template.domain.PreviewComparisonSeverity;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PreviewComparisonService {

    public PreviewComparisonView compare(
            List<AnchorBindingEntity> bindings,
            List<FidelityWarningView> fidelityWarnings
    ) {
        List<PreviewComparisonItemView> items = new ArrayList<>();

        for (AnchorBindingEntity binding : bindings) {
            if (binding.getValidationStatus() == BindingValidationStatus.MISSING_ANCHOR
                    || binding.getValidationStatus() == BindingValidationStatus.INCOMPATIBLE_CONTENT_TYPE) {
                items.add(new PreviewComparisonItemView(
                        PreviewComparisonLocationType.ANCHOR,
                        binding.getAnchorId(),
                        PreviewComparisonSeverity.BLOCKER,
                        "SEMANTIC_BINDING_MISMATCH",
                        "bindingStatus=" + binding.getValidationStatus().name()
                ));
            } else if (binding.getValidationStatus() == BindingValidationStatus.DUPLICATE_BINDING) {
                items.add(new PreviewComparisonItemView(
                        PreviewComparisonLocationType.ANCHOR,
                        binding.getAnchorId(),
                        PreviewComparisonSeverity.WARNING,
                        "DUPLICATE_BINDING",
                        "bindingStatus=" + binding.getValidationStatus().name()
                ));
            }
        }

        if (fidelityWarnings != null) {
            for (FidelityWarningView warning : fidelityWarnings) {
                PreviewComparisonSeverity severity = isSemanticBlocker(warning)
                        ? PreviewComparisonSeverity.BLOCKER
                        : PreviewComparisonSeverity.WARNING;
                items.add(new PreviewComparisonItemView(
                        PreviewComparisonLocationType.COMPONENT,
                        warning.code(),
                        severity,
                        warning.code(),
                        warning.messageKey()
                ));
            }
        }

        int blockerCount = (int) items.stream()
                .filter(item -> item.severity() == PreviewComparisonSeverity.BLOCKER)
                .count();
        int warningCount = items.size() - blockerCount;
        return new PreviewComparisonView(items.size(), blockerCount, warningCount, items);
    }

    private boolean isSemanticBlocker(FidelityWarningView warning) {
        return FidelityWarningCode.UNRESOLVED_VARIABLE.name().equals(warning.code())
                || FidelityWarningCode.MISSING_ANCHOR_CONTENT.name().equals(warning.code());
    }
}
