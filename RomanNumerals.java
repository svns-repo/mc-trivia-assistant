package com.example.triviaassistant;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts between Roman numerals and integers, and detects which
 * direction a trivia question is asking for.
 */
public class RomanNumerals {

    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private static final Map<Character, Integer> CHAR_VALUES = new LinkedHashMap<>();
    static {
        CHAR_VALUES.put('I', 1);
        CHAR_VALUES.put('V', 5);
        CHAR_VALUES.put('X', 10);
        CHAR_VALUES.put('L', 50);
        CHAR_VALUES.put('C', 100);
        CHAR_VALUES.put('D', 500);
        CHAR_VALUES.put('M', 1000);
    }

    // Matches a standalone roman numeral token, e.g. XXVIII, MCMXCIV
    private static final Pattern ROMAN_TOKEN = Pattern.compile("\\b[MDCLXVI]{2,}\\b|\\b[IVXLCDM]\\b");

    public static String toRoman(int num) {
        if (num <= 0 || num > 3999) {
            return null; // standard roman numerals only cover 1-3999
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < VALUES.length; i++) {
            while (num >= VALUES[i]) {
                num -= VALUES[i];
                sb.append(SYMBOLS[i]);
            }
        }
        return sb.toString();
    }

    public static Integer fromRoman(String s) {
        if (s == null || s.isEmpty()) return null;
        s = s.toUpperCase();
        for (char c : s.toCharArray()) {
            if (!CHAR_VALUES.containsKey(c)) return null;
        }
        int result = 0;
        for (int i = 0; i < s.length(); i++) {
            int cur = CHAR_VALUES.get(s.charAt(i));
            int next = (i + 1 < s.length()) ? CHAR_VALUES.get(s.charAt(i + 1)) : 0;
            result += (cur < next) ? -cur : cur;
        }
        return result;
    }

    /**
     * Given a full question string, tries to answer it if it's asking for
     * a roman numeral conversion in either direction. Returns null if this
     * doesn't look like a roman numeral question.
     */
    public static String trySolve(String question) {
        String lower = question.toLowerCase();
        boolean mentionsRoman = lower.contains("roman numeral");

        // Direction 1: "what is XXVIII as a number" / "convert XXVIII to a number"
        if (mentionsRoman || Pattern.compile("\\b(as a number|to (a )?number)\\b").matcher(lower).find()) {
            Matcher m = ROMAN_TOKEN.matcher(question);
            while (m.find()) {
                String token = m.group();
                // Skip common false positives that happen to be valid roman letters (e.g. "I", "V" as words)
                if (token.length() == 1 && !mentionsRoman) continue;
                Integer value = fromRoman(token);
                if (value != null) {
                    return String.valueOf(value);
                }
            }
        }

        // Direction 2: "what is 28 as a roman numeral" / "convert 28 to roman numerals"
        if (mentionsRoman) {
            Matcher numMatch = Pattern.compile("\\b(\\d{1,4})\\b").matcher(question);
            if (numMatch.find()) {
                int value = Integer.parseInt(numMatch.group(1));
                String roman = toRoman(value);
                if (roman != null) {
                    return roman;
                }
            }
        }

        return null;
    }
}
