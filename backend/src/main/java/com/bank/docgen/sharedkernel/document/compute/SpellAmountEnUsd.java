package com.bank.docgen.sharedkernel.document.compute;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * English USD amount-in-words (IBL-A3). Stable forms locked by unit tests, e.g.
 * {@code 1000 → "USD One Thousand Only"}.
 */
final class SpellAmountEnUsd {

    private static final String[] BELOW_TWENTY = {
            "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private static final String[] SCALES = {"", "Thousand", "Million", "Billion", "Trillion"};

    private SpellAmountEnUsd() {
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
        long centsTotal = scaled.movePointRight(2).longValueExact();
        long dollars = centsTotal / 100;
        int cents = (int) (centsTotal % 100);

        StringBuilder builder = new StringBuilder("USD ");
        builder.append(spellInteger(dollars));
        if (cents == 0) {
            builder.append(" Only");
        } else {
            builder.append(" And ").append(spellInteger(cents));
            builder.append(cents == 1 ? " Cent Only" : " Cents Only");
        }
        return builder.toString();
    }

    private static String spellInteger(long value) {
        if (value == 0) {
            return BELOW_TWENTY[0];
        }
        if (value < 0) {
            throw new IllegalArgumentException("negative integer");
        }
        StringBuilder result = new StringBuilder();
        int scaleIndex = 0;
        long remaining = value;
        while (remaining > 0) {
            int chunk = (int) (remaining % 1000);
            if (chunk != 0) {
                String chunkWords = spellChunk(chunk);
                String scale = SCALES[scaleIndex];
                String segment = scale.isEmpty() ? chunkWords : chunkWords + " " + scale;
                if (result.length() == 0) {
                    result.insert(0, segment);
                } else {
                    result.insert(0, segment + " ");
                }
            }
            remaining /= 1000;
            scaleIndex++;
            if (scaleIndex >= SCALES.length && remaining > 0) {
                throw new IllegalArgumentException("amount exceeds english spell scales");
            }
        }
        return result.toString();
    }

    private static String spellChunk(int value) {
        int hundreds = value / 100;
        int remainder = value % 100;
        StringBuilder chunk = new StringBuilder();
        if (hundreds > 0) {
            chunk.append(BELOW_TWENTY[hundreds]).append(" Hundred");
            if (remainder > 0) {
                chunk.append(' ');
            }
        }
        if (remainder > 0) {
            chunk.append(spellBelowHundred(remainder));
        }
        return chunk.toString();
    }

    private static String spellBelowHundred(int value) {
        if (value < 20) {
            return BELOW_TWENTY[value];
        }
        int tens = value / 10;
        int ones = value % 10;
        if (ones == 0) {
            return TENS[tens];
        }
        return TENS[tens] + "-" + BELOW_TWENTY[ones];
    }
}
