package com.bank.docgen.sharedkernel.document.style;

/**
 * Theme font scheme (major/minor latin/ea/cs) from {@code word/theme/theme*.xml}.
 */
public record MasterStyleThemeFonts(
        String majorLatin,
        String majorEastAsia,
        String majorCs,
        String minorLatin,
        String minorEastAsia,
        String minorCs
) {
}
