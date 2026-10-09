package main.walksy.lib.core.network;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.Config;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.toast.WLToast;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ServerOptOuts {
    private static final float NOTICE_SECONDS = 8f;
    private static final int ICON_TEXTURE_SIZE = 32;
    private static final int NOTICE_COOLDOWN_TICKS = 40;
    private final Map<String, Registered> registered = new LinkedHashMap<>();
    private int currentTick;

    private static final class Registered {
        private final Mod mod;
        private final ServerOptOut optOut;
        private final Map<String, String> liftReasons = new LinkedHashMap<>();
        private ServerOptOut.Shown shown = ServerOptOut.Shown.NOTHING;
        private int lastNoticeTick;
        private boolean noticed;
        private boolean pending;

        private Registered(final Mod mod, final ServerOptOut optOut) {
            this.mod = mod;
            this.optOut = optOut;
        }
    }

    public record Row(String modId, String displayName, ServerOptOut optOut) {
    }

    public void register(final Mod mod) {
        final ModContainer container = mod.getContainer();
        final Config config = mod.getConfig();
        if (container == null || config == null || config.serverOptOut().isEmpty()) {
            return;
        }
        this.registered.putIfAbsent(container.getMetadata().getId(), new Registered(mod, config.serverOptOut()));
    }

    public boolean any() {
        return !this.registered.isEmpty();
    }

    public @Nullable ServerOptOut of(final String modId) {
        final Registered found = this.registered.get(modId);
        return found == null ? null : found.optOut;
    }

    public boolean isModDisabled(final String modId) {
        final ServerOptOut optOut = this.of(modId);
        return optOut != null && optOut.isModDisabled();
    }

    public boolean isFeatureDisabled(final String modId, final String feature) {
        final ServerOptOut optOut = this.of(modId);
        return optOut != null && optOut.isFeatureDisabled(feature);
    }

    public List<Row> all() {
        final List<Row> rows = new ArrayList<>(this.registered.size());
        for (final Map.Entry<String, Registered> entry : this.registered.entrySet()) {
            rows.add(new Row(entry.getKey(), entry.getValue().mod.getDisplayName(), entry.getValue().optOut));
        }
        return rows;
    }

    ServerHelloPayload hello() {
        final List<ServerHelloPayload.Entry> mods = new ArrayList<>(this.registered.size());
        for (final Map.Entry<String, Registered> entry : this.registered.entrySet()) {
            final ServerOptOut optOut = entry.getValue().optOut;
            mods.add(new ServerHelloPayload.Entry(entry.getKey(), entry.getValue().mod.getDisplayName(), optOut.allowsWholeMod(), List.copyOf(optOut.features())));
        }
        return new ServerHelloPayload(mods);
    }

    void apply(final ServerControlPayload payload) {
        final Map<String, ServerControlPayload.Entry> byMod = new LinkedHashMap<>();
        for (final ServerControlPayload.Entry entry : payload.entries()) {
            byMod.put(entry.modId(), entry);
        }
        for (final ServerControlPayload.Lifted lifted : payload.lifted()) {
            final Registered found = this.registered.get(lifted.modId());
            if (found == null || lifted.reason().isBlank()) {
                continue;
            }
            final boolean declared = lifted.feature()
                .isEmpty() ? found.optOut.allowsWholeMod() : found.optOut.features()
                .contains(lifted.feature());
            if (declared) {
                found.liftReasons.put(lifted.feature(), lifted.reason());
            }
        }
        for (final Map.Entry<String, Registered> entry : this.registered.entrySet()) {
            final ServerControlPayload.Entry payloadEntry = byMod.get(entry.getKey());
            if (payloadEntry == null) {
                entry.getValue().optOut.apply(entry.getKey(), false, "", Map.of());
            } else {
                final Map<String, String> listed = new LinkedHashMap<>();
                for (final ServerControlPayload.Feature feature : payloadEntry.features()) {
                    listed.put(feature.id(), feature.reason());
                }
                entry.getValue().optOut.apply(entry.getKey(), payloadEntry.disableMod(), payloadEntry.reason(), listed);
            }
            this.announce(entry.getValue());
        }
    }

    void tick() {
        this.currentTick++;
        for (final Registered found : this.registered.values()) {
            if (found.pending && this.currentTick - found.lastNoticeTick >= NOTICE_COOLDOWN_TICKS) {
                this.announce(found);
            }
        }
    }

    void reset() {
        for (final Map.Entry<String, Registered> entry : this.registered.entrySet()) {
            entry.getValue().optOut.apply(entry.getKey(), false, "", Map.of());
            entry.getValue().shown = ServerOptOut.Shown.NOTHING;
            entry.getValue().liftReasons.clear();
            entry.getValue().pending = false;
            entry.getValue().noticed = false;
        }
    }

    private void announce(final Registered found) {
        if (found.noticed && this.currentTick - found.lastNoticeTick < NOTICE_COOLDOWN_TICKS) {
            found.pending = true;
            return;
        }
        found.pending = false;
        final ServerOptOut.Shown current = ServerOptOut.Shown.of(found.optOut);
        if (current.equals(found.shown)) {
            found.liftReasons.clear();
            return;
        }
        this.notifyPlayer(found, found.shown, current);
        found.liftReasons.clear();
        found.shown = current;
        found.lastNoticeTick = this.currentTick;
        found.noticed = true;
    }

    private void notifyPlayer(final Registered found, final ServerOptOut.Shown before, final ServerOptOut.Shown now) {
        final String name = found.mod.getDisplayName();
        final ServerOptOut optOut = found.optOut;
        final List<String> newlyOff = now.newlyOff(before);
        final List<String> newlyOn = before.newlyOff(now);
        final WLToast toast;
        if (!before.modDisabled() && now.modDisabled()) {
            toast = WLToast.of("This server turned off " + name, optOut.modReason().isBlank() ? "It comes back on when you leave." : optOut.modReason()).warning();
        } else if (!newlyOff.isEmpty()) {
            toast = WLToast.of("This server turned off part of " + name, describe(newlyOff, optOut::featureReason, "Off here: ")).warning();
        } else if (before.modDisabled() && !now.modDisabled()) {
            final String why = found.liftReasons.getOrDefault("", "");
            toast = WLToast.of(name + " is back on", !why.isBlank() ? why : now.off().isEmpty() ? "The server lifted its restriction." : "Still off here: " + String.join(", ", now.off())).info();
        } else {
            toast = WLToast.of("Back on in " + name, describe(newlyOn, found.liftReasons::get, "")).info();
        }
        WalksyLib.getInstance()
            .getToastManager()
            .submit(toast.icon(found.mod.getModIcon(), ICON_TEXTURE_SIZE)
                .id("walksylib:server-opt-out:" + found.mod.getContainer().getMetadata().getId())
                .seconds(NOTICE_SECONDS));
    }

    private static String describe(final List<String> features, final Function<String, String> reasons, final String plainPrefix) {
        final List<String> parts = new ArrayList<>(features.size());
        boolean anyReason = false;
        for (final String feature : features) {
            final String found = reasons.apply(feature);
            final String reason = found == null ? "" : found;
            anyReason |= !reason.isBlank();
            parts.add(reason.isBlank() ? feature : feature + ": " + reason);
        }
        return anyReason ? String.join("; ", parts) : plainPrefix + String.join(", ", features);
    }
}
