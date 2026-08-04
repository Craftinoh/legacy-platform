package it.legacynetwork.items.util;

public final class LegacyColorTranslator {
    private static final String COLOR_CHARS = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr";

    private LegacyColorTranslator() {
    }

    public static String translate(String input) {
        if (input == null) {
            return null;
        }
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && COLOR_CHARS.indexOf(chars[i + 1]) >= 0) {
                chars[i] = '\u00A7';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }
}
