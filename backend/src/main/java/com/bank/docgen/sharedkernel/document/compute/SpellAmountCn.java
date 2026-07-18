package com.bank.docgen.sharedkernel.document.compute;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * CNY Chinese uppercase amount — used by unary {@code SPELL_AMOUNT(value)} (always,
 * locale-independent) and by registered binary pair {@code (zh, CNY)} (IBL-A3).
 */
final class SpellAmountCn {

    private static final String[] DIGITS = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
    private static final String[] SMALL_UNITS = {"", "拾", "佰", "仟"};
    private static final String[] SECTION_UNITS = {"", "万", "亿"};

    private SpellAmountCn() {
    }

    static String spell(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount is null");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("negative amount");
        }
        if (amount.compareTo(ComputeDslLimits.MAX_SPELL_AMOUNT) > 0) {
            throw new IllegalArgumentException("amount exceeds max");
        }
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        long fen = scaled.movePointRight(2).longValueExact();
        long yuan = fen / 100;
        int jiao = (int) ((fen / 10) % 10);
        int fenDigit = (int) (fen % 10);

        if (yuan == 0 && jiao == 0 && fenDigit == 0) {
            return "零元整";
        }

        StringBuilder builder = new StringBuilder();
        if (yuan > 0) {
            builder.append(spellInteger(yuan)).append('元');
        } else {
            builder.append("零元");
        }
        if (jiao == 0 && fenDigit == 0) {
            builder.append('整');
        } else {
            if (jiao > 0) {
                builder.append(DIGITS[jiao]).append('角');
            } else if (fenDigit > 0 && yuan > 0) {
                builder.append('零');
            }
            if (fenDigit > 0) {
                builder.append(DIGITS[fenDigit]).append('分');
            }
        }
        return builder.toString();
    }

    private static String spellInteger(long value) {
        if (value == 0) {
            return DIGITS[0];
        }
        StringBuilder result = new StringBuilder();
        String digits = Long.toString(value);
        int len = digits.length();
        boolean zeroPending = false;
        for (int i = 0; i < len; i++) {
            int digit = digits.charAt(i) - '0';
            int posFromRight = len - 1 - i;
            int sectionIndex = posFromRight / 4;
            int withinSection = posFromRight % 4;
            if (digit == 0) {
                zeroPending = result.length() > 0;
            } else {
                if (zeroPending) {
                    result.append(DIGITS[0]);
                    zeroPending = false;
                }
                result.append(DIGITS[digit]).append(SMALL_UNITS[withinSection]);
            }
            if (withinSection == 0 && sectionIndex > 0) {
                boolean sectionHasValue = false;
                int sectionStart = len - (sectionIndex + 1) * 4;
                int sectionEnd = len - sectionIndex * 4;
                if (sectionStart < 0) {
                    sectionStart = 0;
                }
                for (int j = sectionStart; j < sectionEnd; j++) {
                    if (digits.charAt(j) != '0') {
                        sectionHasValue = true;
                        break;
                    }
                }
                if (sectionHasValue) {
                    result.append(SECTION_UNITS[sectionIndex]);
                    zeroPending = false;
                }
            }
        }
        return result.toString();
    }
}
