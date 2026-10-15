package main.walksy.lib.core.platform.compat.loader;

import java.util.ArrayList;
import java.util.List;

public final class Version implements Comparable<Version> {
    private final String friendly;
    private final List<Long> numbers;
    private final String preRelease;

    private Version(final String friendly, final List<Long> numbers, final String preRelease) {
        this.friendly = friendly;
        this.numbers = numbers;
        this.preRelease = preRelease;
    }

    public static Version parse(final String raw) throws VersionParsingException {
        if (raw == null || raw.isBlank()) {
            throw new VersionParsingException("Version is empty");
        }
        final String trimmed = raw.trim();
        String core = trimmed;
        final int build = core.indexOf('+');
        if (build >= 0) {
            core = core.substring(0, build);
        }
        String pre = "";
        final int dash = core.indexOf('-');
        if (dash >= 0) {
            pre = core.substring(dash + 1);
            core = core.substring(0, dash);
        }
        final List<Long> numbers = new ArrayList<>();
        for (final String part : core.split("\\.")) {
            if (part.isEmpty()) {
                throw new VersionParsingException("Version has an empty component: " + raw);
            }
            try {
                numbers.add(Long.parseLong(part));
            } catch (final NumberFormatException e) {
                throw new VersionParsingException("Version component is not a number: " + raw);
            }
        }
        return new Version(trimmed, numbers, pre);
    }

    public String getFriendlyString() {
        return this.friendly;
    }

    @Override
    public int compareTo(final Version other) {
        final int length = Math.max(this.numbers.size(), other.numbers.size());
        for (int i = 0; i < length; i++) {
            final long a = i < this.numbers.size() ? this.numbers.get(i) : 0L;
            final long b = i < other.numbers.size() ? other.numbers.get(i) : 0L;
            if (a != b) {
                return Long.compare(a, b);
            }
        }
        if (this.preRelease.equals(other.preRelease)) {
            return 0;
        }
        if (this.preRelease.isEmpty()) {
            return 1;
        }
        if (other.preRelease.isEmpty()) {
            return -1;
        }
        return this.preRelease.compareTo(other.preRelease);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Version other && this.compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        return this.numbers.hashCode() * 31 + this.preRelease.hashCode();
    }

    @Override
    public String toString() {
        return this.friendly;
    }
}
