package com.bank.docgen.library.api;

/** Non-sensitive actor summary in the library-export manifest (E03-C11). */
public record LibraryExportActorView(
        String userId,
        String role
) {
}
