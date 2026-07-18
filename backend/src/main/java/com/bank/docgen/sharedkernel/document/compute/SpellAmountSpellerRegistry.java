package com.bank.docgen.sharedkernel.document.compute;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Extensible (language × currency) amount-in-words registry (IBL-A3 / A3-C13).
 *
 * <p>New pairs are added by registering a {@link SpellAmountSpeller}; DSL arity and
 * function name stay unchanged. Unregistered pairs fail closed (no silent fallback).
 */
final class SpellAmountSpellerRegistry {

    private record PairKey(String language, String currency) {
    }

    private static final Map<PairKey, SpellAmountSpeller> SPELLERS = Map.of(
            new PairKey("zh", "CNY"), SpellAmountCn::spell,
            new PairKey("en", "USD"), SpellAmountEnUsd::spell
    );

    private SpellAmountSpellerRegistry() {
    }

    static boolean isSupported(String language, String currency) {
        return SPELLERS.containsKey(normalizeKey(language, currency));
    }

    static SpellAmountSpeller require(String language, String currency) {
        PairKey key = normalizeKey(language, currency);
        SpellAmountSpeller speller = SPELLERS.get(key);
        if (speller == null) {
            throw new IllegalArgumentException(
                    "unsupported SPELL_AMOUNT language/currency pair: "
                            + key.language() + "/" + key.currency()
            );
        }
        return speller;
    }

    private static PairKey normalizeKey(String language, String currency) {
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(currency, "currency");
        String lang = language.trim().toLowerCase(Locale.ROOT);
        String ccy = currency.trim().toUpperCase(Locale.ROOT);
        if (lang.isEmpty() || ccy.isEmpty()) {
            throw new IllegalArgumentException("SPELL_AMOUNT language/currency blank");
        }
        return new PairKey(lang, ccy);
    }
}
