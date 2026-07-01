package org.atrimilan.sidastuffsmp.utils;

import java.util.Locale;

public final class AmountParser {

    private AmountParser() {}

    public static double parse(String input) throws IllegalArgumentException {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Amount cannot be empty");
        }

        String cleaned = input.trim().toLowerCase(Locale.ROOT).replace(",", "");

        try {
            double multiplier = 1.0;
            if (cleaned.endsWith("k")) {
                multiplier = 1_000.0;
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            } else if (cleaned.endsWith("m")) {
                multiplier = 1_000_000.0;
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            } else if (cleaned.endsWith("b")) {
                multiplier = 1_000_000_000.0;
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            } else if (cleaned.endsWith("t")) {
                multiplier = 1_000_000_000_000.0;
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }

            double value = Double.parseDouble(cleaned);
            double result = value * multiplier;

            if (Double.isInfinite(result) || Double.isNaN(result)) {
                throw new IllegalArgumentException("Amount is too large");
            }

            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount: " + input + ". Use numbers like 100, 10k, 1.5m, etc.");
        }
    }
}
