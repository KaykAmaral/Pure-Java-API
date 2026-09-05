package com.project.jobs.json;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class FlatJsonObjectParser {

    private FlatJsonObjectParser() {
    }

    public static Map<String, String> parse(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return new Parser(text).parseObject();
    }

    private static final class Parser {

        private final String text;
        private int position;

        private Parser(String text) {
            this.text = text;
        }

        private Map<String, String> parseObject() {
            Map<String, String> fields = new HashMap<>();

            skipWhitespace();
            expect('{');
            skipWhitespace();

            if (consumeIf('}')) {
                ensureEndOfInput();
                return Map.copyOf(fields);
            }

            while (true) {
                String key = readString();

                skipWhitespace();
                expect(':');
                skipWhitespace();

                String value = readString();

                if (fields.putIfAbsent(key, value) != null) {
                    throw error("duplicate key: " + key);
                }

                skipWhitespace();

                if (consumeIf('}')) {
                    ensureEndOfInput();
                    return Map.copyOf(fields);
                }

                expect(',');
                skipWhitespace();
            }
        }

        private String readString() {
            expect('"');

            StringBuilder value = new StringBuilder();

            while (!isAtEnd()) {
                char current = text.charAt(position++);

                if (current == '"') {
                    return value.toString();
                }

                if (current == '\\') {
                    value.append(readEscape());
                    continue;
                }

                if (current <= 0x1F) {
                    throw error("unescaped control character inside string");
                }

                value.append(current);
            }

            throw error("unterminated string");
        }

        private char readEscape() {
            if (isAtEnd()) {
                throw error("unfinished escape sequence");
            }

            char escaped = text.charAt(position++);

            return switch (escaped) {
                case '"' -> '"';
                case '\\' -> '\\';
                case '/' -> '/';
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'u' -> readUnicodeEscape();
                default -> throw error("invalid escape sequence: \\" + escaped);
            };
        }

        private char readUnicodeEscape() {
            int value = 0;

            for (int digitIndex = 0; digitIndex < 4; digitIndex++) {
                if (isAtEnd()) {
                    throw error("incomplete unicode escape");
                }

                char current = text.charAt(position++);
                int hexadecimalDigit = Character.digit(current, 16);

                if (hexadecimalDigit == -1) {
                    throw error("invalid hexadecimal digit: " + current);
                }

                value = value * 16 + hexadecimalDigit;
            }

            return (char) value;
        }

        private void skipWhitespace() {
            while (!isAtEnd()) {
                char current = text.charAt(position);

                if (current != ' ' && current != '\t' && current != '\r' && current != '\n') {
                    return;
                }

                position++;
            }
        }

        private void expect(char expected) {
            if (isAtEnd() || text.charAt(position) != expected) {
                throw error("expected '" + expected + "'");
            }

            position++;
        }

        private boolean consumeIf(char expected) {
            if (!isAtEnd() && text.charAt(position) == expected) {
                position++;
                return true;
            }

            return false;
        }

        private void ensureEndOfInput() {
            skipWhitespace();

            if (!isAtEnd()) {
                throw error("unexpected content after object");
            }
        }

        private boolean isAtEnd() {
            return position >= text.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(
                    "invalid JSON at position " + position + ": " + message
            );
        }
    }
}
