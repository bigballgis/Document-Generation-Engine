package com.bank.docgen.sharedkernel.document.style;

/**
 * Document defaults from {@code w:docDefaults} in master {@code styles.xml}.
 */
public record MasterStyleDocDefaults(
        String ascii,
        String hAnsi,
        String eastAsia,
        String cs,
        Integer fontSizeHalfPoints,
        String color
) {

    public boolean hasAnyFontSlot() {
        return isPresent(ascii) || isPresent(hAnsi) || isPresent(eastAsia) || isPresent(cs);
    }

    public boolean hasFontSize() {
        return fontSizeHalfPoints != null && fontSizeHalfPoints > 0;
    }

    public MasterStyleTypography toTypography() {
        return new MasterStyleTypography(ascii, hAnsi, eastAsia, cs, fontSizeHalfPoints, null, null, color);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
