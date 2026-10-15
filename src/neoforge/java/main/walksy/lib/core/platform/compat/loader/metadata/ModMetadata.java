package main.walksy.lib.core.platform.compat.loader.metadata;

import main.walksy.lib.core.platform.compat.loader.Version;
import main.walksy.lib.core.platform.compat.loader.VersionParsingException;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModMetadata {
    private final IModInfo info;
    private final Map<String, Object> properties;

    public ModMetadata(final IModInfo info) {
        this.info = info;
        Map<String, Object> read;
        try {
            read = info.getModProperties();
        } catch (final Throwable t) {
            read = Map.of();
        }
        this.properties = read == null ? Map.of() : read;
    }

    public String getId() {
        return this.info.getModId();
    }

    public String getName() {
        final String name = this.info.getDisplayName();
        return name == null || name.isBlank() ? this.info.getModId() : name;
    }

    public Version getVersion() {
        final String raw = String.valueOf(this.info.getVersion());
        try {
            return Version.parse(raw);
        } catch (final VersionParsingException e) {
            try {
                return Version.parse("0");
            } catch (final VersionParsingException impossible) {
                throw new IllegalStateException(impossible);
            }
        }
    }

    public String getDescription() {
        final String description = this.info.getDescription();
        return description == null ? "" : description;
    }

    public List<Person> getAuthors() {
        final List<Person> authors = new ArrayList<>();
        final Object raw = this.properties.get("authors");
        if (raw != null) {
            for (final String name : String.valueOf(raw).split(",")) {
                final String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    authors.add(() -> trimmed);
                }
            }
        }
        return authors;
    }

    public ContactInformation getContact() {
        final Map<String, String> values = new LinkedHashMap<>();
        for (final String key : new String[]{"sources", "homepage", "issues"}) {
            final Object value = this.properties.get(key);
            if (value != null) {
                values.put(key, String.valueOf(value));
            }
        }
        return new ContactInformation(values);
    }

    public Optional<String> getIconPath(final int preferredSize) {
        try {
            return this.info.getLogoFile();
        } catch (final Throwable t) {
            return Optional.empty();
        }
    }
}
