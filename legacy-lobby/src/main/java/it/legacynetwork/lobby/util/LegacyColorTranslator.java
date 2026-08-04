package it.legacynetwork.lobby.util;

public final class LegacyColorTranslator {
    private static final String COLOR_CHARACTERS = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr";

    private LegacyColorTranslator() {
    }

    public static String translate(String input) {
        if (input == null) {
            return null;
        }
        char[] characters = input.toCharArray();
        for (int index = 0; index < characters.length - 1; index++) {
            if (characters[index] == '&'
                    && COLOR_CHARACTERS.indexOf(characters[index + 1]) >= 0) {
                characters[index] = '\u00A7';
                characters[index + 1] =
                        Character.toLowerCase(characters[index + 1]);
            }
        }
        return new String(characters);
    }
}
