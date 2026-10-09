package main.walksy.lib.core.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LegacyText {
    public static final char SECTION = ChatFormatting.PREFIX_CODE;
    public static final char AMPERSAND = '&';
    public static final String CODE_CHARS = "0123456789abcdefklmnor";
    private static final Map<ChatFormatting, Character> CODES = new HashMap<>();

    static {
        for (int i = 0; i < CODE_CHARS.length(); i++) {
            final char code = CODE_CHARS.charAt(i);
            final ChatFormatting formatting = ChatFormatting.getByCode(code);
            if (formatting != null) {
                CODES.putIfAbsent(formatting, code);
            }
        }
    }

    private LegacyText() {
    }

    public static Component toComponent(final String legacy) {
        if (legacy == null || legacy.isEmpty()) {
            return null;
        }
        final MutableComponent root = Component.empty().withStyle(Style.EMPTY.withItalic(false));
        Style style = Style.EMPTY;
        final StringBuilder run = new StringBuilder();
        for (int i = 0; i < legacy.length(); i++) {
            final char c = legacy.charAt(i);
            final ChatFormatting formatting = (c == SECTION || c == AMPERSAND) && i + 1 < legacy.length() ? ChatFormatting.getByCode(legacy.charAt(i + 1)) : null;
            if (formatting == null) {
                run.append(c);
                continue;
            }
            if (run.length() > 0) {
                root.append(Component.literal(run.toString()).withStyle(style));
                run.setLength(0);
            }
            style = formatting == ChatFormatting.RESET ? Style.EMPTY : style.applyFormat(formatting);
            i++;
        }
        if (run.length() > 0) {
            root.append(Component.literal(run.toString()).withStyle(style));
        }
        return root;
    }

    public static String fromComponent(final Component component) {
        if (component == null) {
            return "";
        }
        final StringBuilder out = new StringBuilder();
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                out.append(codesFor(style)).append(text);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out.toString();
    }

    private static String codesFor(final Style style) {
        final StringBuilder codes = new StringBuilder();
        append(codes, ChatFormatting.RESET);
        if (style.getColor() != null) {
            for (final ChatFormatting formatting : ChatFormatting.values()) {
                if (style.getColor().equals(TextColor.fromLegacyFormat(formatting))) {
                    append(codes, formatting);
                    break;
                }
            }
        }
        if (style.isBold()) {
            append(codes, ChatFormatting.BOLD);
        }
        if (style.isItalic()) {
            append(codes, ChatFormatting.ITALIC);
        }
        if (style.isUnderlined()) {
            append(codes, ChatFormatting.UNDERLINE);
        }
        if (style.isStrikethrough()) {
            append(codes, ChatFormatting.STRIKETHROUGH);
        }
        if (style.isObfuscated()) {
            append(codes, ChatFormatting.OBFUSCATED);
        }
        return codes.toString();
    }

    private static void append(final StringBuilder out, final ChatFormatting formatting) {
        final Character code = CODES.get(formatting);
        if (code != null) {
            out.append(SECTION).append(code.charValue());
        }
    }

    public static String strip(final String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        final StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if ((c == SECTION || c == AMPERSAND) && i + 1 < text.length() && ChatFormatting.getByCode(text.charAt(i + 1)) != null) {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    public static int colorOf(final String legacy, final int fallback) {
        if (legacy == null) {
            return fallback;
        }
        int color = fallback;
        for (int i = 0; i + 1 < legacy.length(); i++) {
            final char c = legacy.charAt(i);
            if (c != SECTION && c != AMPERSAND) {
                continue;
            }
            final ChatFormatting formatting = ChatFormatting.getByCode(legacy.charAt(i + 1));
            if (formatting == null) {
                continue;
            }
            i++;
            if (formatting == ChatFormatting.RESET) {
                color = fallback;
            } else {
                final TextColor text = TextColor.fromLegacyFormat(formatting);
                if (text != null) {
                    color = 0xFF000000 | text.getValue();
                }
            }
        }
        return color;
    }

    public static List<Swatch> colorSwatches() {
        final List<Swatch> swatches = new ArrayList<>();
        for (int i = 0; i < CODE_CHARS.length(); i++) {
            final char code = CODE_CHARS.charAt(i);
            final ChatFormatting formatting = ChatFormatting.getByCode(code);
            if (formatting == null) {
                continue;
            }
            final TextColor color = TextColor.fromLegacyFormat(formatting);
            if (color != null) {
                swatches.add(new Swatch(code, 0xFF000000 | color.getValue()));
            }
        }
        return List.copyOf(swatches);
    }

    public record Swatch(char code, int argb) {
    }
}
