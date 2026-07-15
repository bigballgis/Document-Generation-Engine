package com.bank.docgen.master.api;

/**
 * Anchor stable-key rename between two master revisions (CE-K05).
 */
public record MasterRenamedAnchorView(String fromAnchorKey, String toAnchorKey) {
}
