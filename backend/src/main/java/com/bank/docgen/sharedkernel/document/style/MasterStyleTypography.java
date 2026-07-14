package com.bank.docgen.sharedkernel.document.style;

/**
 * Run-level typography extracted from master {@code styles.xml} (or theme resolution).
 */
public record MasterStyleTypography(
        String ascii,
        String hAnsi,
        String eastAsia,
        String cs,
        Integer fontSizeHalfPoints,
        Boolean bold,
        Boolean italic,
        String color
) {

    public boolean hasAnyFontSlot() {
        return isPresent(ascii) || isPresent(hAnsi) || isPresent(eastAsia) || isPresent(cs);
    }

    public boolean hasFontSize() {
        return fontSizeHalfPoints != null && fontSizeHalfPoints > 0;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
