package com.bank.docgen.authoring.structured;

import java.util.Set;

final class DirectFormatRules {

    static final Set<String> WHITELIST = Set.of(
            "fontFamily",
            "fontSize",
            "textColor",
            "lineSpacing",
            "spacingBefore",
            "spacingAfter",
            "firstLineIndent",
            "leftIndent",
            "rightIndent"
    );

    static final Set<String> GLOBAL_LAYOUT = Set.of(
            "pageMarginTop",
            "pageMarginBottom",
            "pageMarginLeft",
            "pageMarginRight",
            "headerDistance",
            "footerDistance",
            "paperSize",
            "paperWidth",
            "paperHeight",
            "columnCount",
            "pageSetup",
            "globalLayout"
    );

    private DirectFormatRules() {
    }
}
