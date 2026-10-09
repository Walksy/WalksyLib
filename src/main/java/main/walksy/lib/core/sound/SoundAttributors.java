package main.walksy.lib.core.sound;

import main.walksy.lib.api.sound.SoundAttributor;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SoundAttributors {
    private final List<SoundAttributor<?>> attributors = new CopyOnWriteArrayList<>();

    public void register(final SoundAttributor<?> attributor) {
        if (!this.attributors.contains(attributor)) {
            this.attributors.add(attributor);
        }
    }

    public void unregister(final SoundAttributor<?> attributor) {
        this.attributors.remove(attributor);
    }

    public void onSound(final Holder<SoundEvent> sound, final double x, final double y, final double z) {
        final Vec3 position = new Vec3(x, y, z);
        for (final SoundAttributor<?> attributor : this.attributors) {
            attributor.onSound(sound, position);
        }
    }

    public void tick() {
        for (final SoundAttributor<?> attributor : this.attributors) {
            attributor.tick();
        }
    }

    public void clear() {
        for (final SoundAttributor<?> attributor : this.attributors) {
            attributor.clear();
        }
    }
}
