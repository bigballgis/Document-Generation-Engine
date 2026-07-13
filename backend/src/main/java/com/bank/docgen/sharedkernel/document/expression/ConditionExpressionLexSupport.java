package com.bank.docgen.sharedkernel.document.expression;

import java.math.BigDecimal;

/**
 * Package-private lexer helpers for {@link ConditionExpressionParser}.
 */
final class ConditionExpressionLexSupport {

    private final String input;
    private int pos;

    ConditionExpressionLexSupport(String input) {
        this.input = input;
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

    String parseQuotedString() {
        expect("'");
        StringBuilder builder = new StringBuilder();
        String result = null;
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (ch == '\'') {
                if (pos + 1 < input.length() && input.charAt(pos + 1) == '\'') {
                    builder.append('\'');
                    pos += 2;
                    continue;
                }
                pos++;
                result = builder.toString();
                break;
            }
            builder.append(ch);
            pos++;
        }
        if (result != null) {
            return result;
        }
        throw error("Unterminated string literal");
    }

    BigDecimal parseNumber() {
        skipWhitespace();
        int start = pos;
        if (pos >= input.length() || !Character.isDigit(input.charAt(pos))) {
            throw error("Expected number or string literal");
        }
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            pos++;
        }
        if (pos < input.length() && input.charAt(pos) == '.') {
            pos++;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                pos++;
            }
        }
        String numberText = input.substring(start, pos);
        try {
            return new BigDecimal(numberText);
        } catch (NumberFormatException ex) {
            throw error("Invalid number literal");
        }
    }

    ConditionExpressionParser.ParseException error(String message) {
        return new ConditionExpressionParser.ParseException(message + " at position " + pos);
    }
}
