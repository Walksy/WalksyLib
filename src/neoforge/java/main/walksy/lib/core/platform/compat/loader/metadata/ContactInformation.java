package main.walksy.lib.core.platform.compat.loader.metadata;

import java.util.Map;
import java.util.Optional;

public final class ContactInformation {
    private final Map<String, String> values;

    ContactInformation(final Map<String, String> values) {
        this.values = values;
    }

    public Optional<String> get(final String key) {
        final String value = this.values.get(key);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    public Map<String, String> asMap() {
        return this.values;
    }
}
