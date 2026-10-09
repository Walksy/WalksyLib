package main.walksy.lib.api.config.option;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@FunctionalInterface
public interface EntryNameRule {

    String clean(String candidate);

    EntryNameRule NONE = candidate -> candidate;

    EntryNameRule NO_SPACES = candidate -> candidate.replace(" ", "");

    static EntryNameRule spacesTo(final char separator) {
        final String replacement = Matcher.quoteReplacement(String.valueOf(separator));
        return candidate -> candidate.trim().replaceAll("\\s+", replacement);
    }

    static EntryNameRule allow(final String allowed) {
        final Pattern disallowed = Pattern.compile("[^" + allowed + "]");
        return candidate -> disallowed.matcher(candidate).replaceAll("");
    }

    default EntryNameRule maxLength(final int max) {
        return candidate -> {
            final String cleaned = this.clean(candidate);
            return cleaned.length() > max ? cleaned.substring(0, Math.max(0, max)) : cleaned;
        };
    }

    default EntryNameRule and(final EntryNameRule next) {
        return candidate -> next.clean(this.clean(candidate));
    }
}
