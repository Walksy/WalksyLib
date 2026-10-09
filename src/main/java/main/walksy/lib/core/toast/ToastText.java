package main.walksy.lib.core.toast;

import main.walksy.lib.core.render.font.msdf.MsdfFont;

import java.util.ArrayList;
import java.util.List;

final class ToastText {
    private ToastText() {
    }

    static List<String> wrap(final MsdfFont font, final String text, final float size, final float maxWidth, final int maxLines) {
        final List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank() || maxWidth <= 0f || maxLines <= 0) {
            return lines;
        }
        final String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            final String word = words[i];
            final String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate, size) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (!line.isEmpty()) {
                if (lines.size() + 1 >= maxLines) {
                    return finish(font, lines, line.toString(), remainder(words, i), size, maxWidth);
                }
                lines.add(line.toString());
                line.setLength(0);
            }
            if (font.width(word, size) <= maxWidth) {
                line.append(word);
                continue;
            }
            if (lines.size() + 1 >= maxLines) {
                return finish(font, lines, word, "", size, maxWidth);
            }
            line.append(font.truncate(word, size, maxWidth));
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static List<String> finish(final MsdfFont font, final List<String> lines, final String head, final String tail, final float size, final float maxWidth) {
        final String last = tail.isEmpty() ? head : head + " " + tail;
        lines.add(font.truncate(last, size, maxWidth));
        return lines;
    }

    private static String remainder(final String[] words, final int from) {
        return String.join(" ", List.of(words).subList(from, words.length));
    }
}
