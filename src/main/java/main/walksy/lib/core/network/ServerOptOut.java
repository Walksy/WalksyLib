package main.walksy.lib.core.network;

import main.walksy.lib.core.WalksyLib;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class ServerOptOut {
    public static final int MAX_FEATURE_LENGTH = 64;
    private static final Pattern FEATURE_ID = Pattern.compile("[a-z0-9_.-]+");
    private final @Nullable ServerOptOutResponse modResponse;
    private final Map<String, ServerOptOutResponse> features;
    private volatile boolean modDisabled;
    private volatile String modReason = "";
    private volatile Map<String, String> listed = Map.of();

    public ServerOptOut(final @Nullable ServerOptOutResponse modResponse, final Map<String, ServerOptOutResponse> features) {
        this.modResponse = modResponse;
        this.features = Collections.unmodifiableMap(new LinkedHashMap<>(features));
    }

    public static String checkFeatureId(final String feature) {
        if (feature == null || feature.isEmpty() || feature.length() > MAX_FEATURE_LENGTH || !FEATURE_ID.matcher(feature).matches()) {
            throw new IllegalArgumentException("Server feature id \"" + feature + "\" must be 1-" + MAX_FEATURE_LENGTH + " characters of a-z, 0-9, '_', '.' or '-'");
        }
        return feature;
    }

    public boolean isEmpty() {
        return this.modResponse == null && this.features.isEmpty();
    }

    public boolean allowsWholeMod() {
        return this.modResponse != null;
    }

    public Set<String> features() {
        return this.features.keySet();
    }

    public boolean isModDisabled() {
        return this.modDisabled;
    }

    public boolean isFeatureDisabled(final String feature) {
        return this.features.containsKey(feature) && (this.modDisabled || this.listed.containsKey(feature));
    }

    public boolean isAnythingDisabled() {
        return this.modDisabled || !this.listed.isEmpty();
    }

    public String modReason() {
        return this.modReason;
    }

    public String featureReason(final String feature) {
        final String own = this.listed.get(feature);
        if (own != null && !own.isBlank()) {
            return own;
        }
        return this.isFeatureDisabled(feature) ? this.modReason : "";
    }

    void apply(final String modId, final boolean disableMod, final String modReason, final Map<String, String> listed) {
        final boolean wasModDisabled = this.modDisabled;
        final boolean nowModDisabled = disableMod && this.modResponse != null;
        final Set<String> before = this.effectivelyDisabled();
        final Map<String, String> kept = new LinkedHashMap<>();
        for (final Map.Entry<String, String> feature : listed.entrySet()) {
            if (this.features.containsKey(feature.getKey())) {
                kept.put(feature.getKey(), feature.getValue() == null ? "" : feature.getValue());
            }
        }
        this.modDisabled = nowModDisabled;
        this.modReason = nowModDisabled && modReason != null ? modReason : "";
        this.listed = Collections.unmodifiableMap(kept);
        if (wasModDisabled != nowModDisabled) {
            run(modId, null, this.modResponse, nowModDisabled);
        }
        final Set<String> after = this.effectivelyDisabled();
        for (final Map.Entry<String, ServerOptOutResponse> entry : this.features.entrySet()) {
            final boolean now = after.contains(entry.getKey());
            if (now != before.contains(entry.getKey())) {
                run(modId, entry.getKey(), entry.getValue(), now);
            }
        }
    }

    Set<String> effectivelyDisabled() {
        final Set<String> off = new LinkedHashSet<>();
        for (final String feature : this.features.keySet()) {
            if (this.isFeatureDisabled(feature)) {
                off.add(feature);
            }
        }
        return off;
    }

    private static void run(final String modId, final @Nullable String feature, final @Nullable ServerOptOutResponse response, final boolean disabled) {
        if (response == null) {
            return;
        }
        try {
            response.respond(disabled);
        } catch (final RuntimeException e) {
            WalksyLib.getLogger().err("Server opt-out response for " + modId + (feature == null ? "" : " feature \"" + feature + "\"") + " threw: " + e);
        }
    }

    record Shown(boolean modDisabled, Set<String> off) {
        static final Shown NOTHING = new Shown(false, Set.of());

        Shown {
            off = Collections.unmodifiableSet(new LinkedHashSet<>(off));
        }

        static Shown of(final ServerOptOut optOut) {
            return new Shown(optOut.isModDisabled(), optOut.effectivelyDisabled());
        }

        List<String> newlyOff(final Shown before) {
            return this.off.stream().filter(feature -> !before.off.contains(feature)).toList();
        }
    }
}
