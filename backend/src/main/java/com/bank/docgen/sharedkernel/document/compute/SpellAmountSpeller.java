package com.bank.docgen.sharedkernel.document.compute;

import java.math.BigDecimal;

/**
 * Amount-in-words speller for one registered (language × currency) pair (IBL-A3).
 */
@FunctionalInterface
interface SpellAmountSpeller {

    String spell(BigDecimal amount);
}
