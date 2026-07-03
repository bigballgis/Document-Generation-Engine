package com.bank.docgen.template.event;

import java.util.UUID;
import org.springframework.context.ApplicationEvent;

/**
 * Published when template content (variable schema, anchor binding, composition rules) changes.
 * Used by other modules to react to content mutations without circular dependencies.
 */
public class TemplateContentChangedEvent extends ApplicationEvent {

    private final UUID templateId;

    public TemplateContentChangedEvent(Object source, UUID templateId) {
        super(source);
        this.templateId = templateId;
    }

    public UUID getTemplateId() {
        return templateId;
    }
}
