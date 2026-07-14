package com.bank.docgen.sharedkernel.document.compute;

import java.math.BigDecimal;

/**
 * Package-private lexer helpers for the whitelist compute DSL.
 */
final class ComputeExpressionLexSupport {

    private final String input;
    private int pos;

    ComputeExpressionLexSupport(String input) {
        this.input = input == null ? "" : input;
    }

    int position() {
        return pos;
    }

    void setPosition(int pos) {
        this.pos = pos;
    }

    String input() {
        return input;
    }

    void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    boolean tryConsume(String token) {
        skipWhitespace();
        if (input.regionMatches(pos, token, 0, token.length())) {
            if (token.chars().allMatch(Character::isLetter)) {
                int end = pos + token.length();
                if (end < input.length()) {
                    char next = input.charAt(end);
                    if (Character.isLetterOrDigit(next) || next == '_') {
                        return false;
                    }
                }
            }
            pos += token.length();
            return true;
        }
        return false;
    }

    void expect(String token) {
        if (!tryConsume(token)) {
            throw error("Expected '" + token + "'");
        }
    }

    boolean startsIdentifier() {
        skipWhitespace();
        if (pos >= input.length()) {
            return false;
        }
        char ch = input.charAt(pos);
        return Character.isLetter(ch) || ch == '_';
    }

    String parseIdentifier() {
        skipWhitespace();
        int start = pos;
        if (pos >= input.length()) {
            throw error("Expected identifier");
        }
        char first = input.charAt(pos);
        if (!(Character.isLetter(first) || first == '_')) {
            throw error("Expected identifier");
        }
        pos++;
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (!(Character.isLetterOrDigit(ch) || ch == '_')) {
                break;
            }
            pos++;
        }
        return input.substring(start, pos);
    }

    String parseFieldPath() {
        skipWhitespace();
        StringBuilder builder = new StringBuilder();
        builder.append(parseIdentifier());
        int segments = 1;
        while (true) {
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == '.') {
                pos++;
                segments++;
                if (segments > ComputeDslLimits.MAX_PATH_SEGMENTS) {
                    throw error("Field path exceeds max segments");
                }
                builder.append('.').append(parseIdentifier());
            } else {
                break;
            }
        }
        return builder.toString();
    }

    String parseVariablePath() {
        expect("${");
        skipWhitespace();
        StringBuilder builder = new StringBuilder();
        builder.append(parseIdentifier());
        int segments = 1;
        while (true) {
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == '.') {
                pos++;
                segments++;
                if (segments > ComputeDslLimits.MAX_PATH_SEGMENTS) {
                    throw error("Variable path exceeds max segments");
                }
                builder.append('.').append(parseIdentifier());
            } else {
                break;
            }
        }
        expect("}");
        return builder.toString();
    }

    BigDecimal parseNumber() {
        skipWhitespace();
        int start = pos;
        if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
            pos++;
        }
        boolean hasDigit = false;
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            hasDigit = true;
            pos++;
        }
        if (pos < input.length() && input.charAt(pos) == '.') {
            pos++;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                hasDigit = true;
                pos++;
            }
        }
        if (!hasDigit) {
            throw error("Expected number");
        }
        return new BigDecimal(input.substring(start, pos));
    }

    String parseQuotedString() {
        skipWhitespace();
        if (pos >= input.length()) {
            throw error("Expected string");
        }
        char quote = input.charAt(pos);
        if (quote != '\'' && quote != '"') {
            throw error("Expected quoted string");
        }
        pos++;
        StringBuilder builder = new StringBuilder();
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (ch == quote) {
                if (pos + 1 < input.length() && input.charAt(pos + 1) == quote) {
                    builder.append(quote);
                    pos += 2;
                    continue;
                }
                pos++;
                return builder.toString();
            }
            builder.append(ch);
            pos++;
        }
        throw error("Unterminated string literal");
    }

    ParseException error(String message) {
        return new ParseException(message + " at position " + pos);
    }

    static final class ParseException extends RuntimeException {
        ParseException(String message) {
            super(message);
        }
    }
}
