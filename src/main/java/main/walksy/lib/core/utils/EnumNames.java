package main.walksy.lib.core.utils;

import java.util.Locale;

public final class EnumNames {
    private EnumNames() {
    }

    public static String pretty(final Enum<?> value) {
        if (value == null) {
            return "";
        }
        final String name = value.name();
        if (!name.equals(name.toUpperCase(Locale.ROOT))) {
            return name;
        }
        final StringBuilder out = new StringBuilder(name.length());
        boolean startOfWord = true;
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (c == '_') {
                out.append(' ');
                startOfWord = true;
                continue;
            }
            out.append(startOfWord ? Character.toUpperCase(c) : Character.toLowerCase(c));
            startOfWord = false;
        }
        return out.toString();
    }
}
