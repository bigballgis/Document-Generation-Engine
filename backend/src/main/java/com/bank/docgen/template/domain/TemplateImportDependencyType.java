package com.bank.docgen.template.domain;

public enum TemplateImportDependencyType {
    MASTER_PIN,
    CLAUSE,
    ASSET_KEY,
    RENDER_PROFILE,
    BUNDLE_FORMAT,
    /** Wave 7 additive — nesting closure coverage (PP-C10). */
    CLAUSE_NESTING,
    /** Wave 7 additive — embedded asset binary disposition (PP-C10). */
    ASSET_BINARY
}
