package com.example.triviaassistant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts and evaluates a simple arithmetic expression from a question
 * string, e.g. "What is 66 / 11?" -> "6".
 *
 * Supports + - * / ^ ( ) and decimal numbers. No external library needed.
 */
public class MathSolver {

    // Matches things like "66 / 11", "12+7*3", "(4+5)/3", "2^10"
    private static final Pattern EXPR_PATTERN =
            Pattern.compile("-?\\d+(?:\\.\\d+)?(?:\\s*[-+*/^]\\s*\\(?\\s*-?\\d+(?:\\.\\d+)?\\)?)+");

    public static String trySolve(String question) {
        Matcher m = EXPR_PATTERN.matcher(question.replace("x", "*").replace("X", "*"));
        if (!m.find()) return null;
        String expr = m.group();
        try {
            double result = new Parser(expr).parse();
            if (Double.isNaN(result) || Double.isInfinite(result)) return null;
            if (result == Math.floor(result)) {
                return String.valueOf((long) result);
            }
            // round to 4 decimal places, trim trailing zeros
            double rounded = Math.round(result * 10000.0) / 10000.0;
            String s = String.valueOf(rounded);
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    /** Minimal recursive-descent parser for + - * / ^ and parens. */
    private static class Parser {
        private final String input;
        private int pos = 0;

        Parser(String input) {
            this.input = input.replaceAll("\\s+", "");
        }

        double parse() {
            double result = parseExpression();
            return result;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (pos < input.length() && (peek() == '+' || peek() == '-')) {
                char op = next();
                double rhs = parseTerm();
                value = (op == '+') ? value + rhs : value - rhs;
            }
            return value;
        }

        private double parseTerm() {
            double value = parseFactor();
            while (pos < input.length() && (peek() == '*' || peek() == '/')) {
                char op = next();
                double rhs = parseFactor();
                value = (op == '*') ? value * rhs : value / rhs;
            }
            return value;
        }

        private double parseFactor() {
            double value = parseUnary();
            if (pos < input.length() && peek() == '^') {
                next();
                double exponent = parseFactor(); // right-associative
                value = Math.pow(value, exponent);
            }
            return value;
        }

        private double parseUnary() {
            if (pos < input.length() && peek() == '-') {
                next();
                return -parseUnary();
            }
            return parsePrimary();
        }

        private double parsePrimary() {
            if (pos < input.length() && peek() == '(') {
                next();
                double value = parseExpression();
                if (pos < input.length() && peek() == ')') next();
                return value;
            }
            int start = pos;
            while (pos < input.length() && (Character.isDigit(peek()) || peek() == '.')) {
                pos++;
            }
            if (start == pos) throw new IllegalArgumentException("Bad expression: " + input);
            return Double.parseDouble(input.substring(start, pos));
        }

        private char peek() {
            return input.charAt(pos);
        }

        private char next() {
            return input.charAt(pos++);
        }
    }
}
