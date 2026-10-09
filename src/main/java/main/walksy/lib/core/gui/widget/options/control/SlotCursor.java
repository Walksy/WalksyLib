package main.walksy.lib.core.gui.widget.options.control;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.api.config.value.ItemSpec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SlotCursor {
    private final Set<Integer> spread = new LinkedHashSet<>();
    private int source = -1;
    private int count;
    private int button = -1;
    private @Nullable ItemSpec loose;
    private @Nullable List<ItemSpec> pending;
    private @Nullable List<ItemSpec> base;

    public interface Rules {
        boolean accepts(int slot, ItemSpec spec);

        int limit(int slot, ItemSpec spec);
    }

    public boolean holding() {
        return this.loose != null || (this.source >= 0 && this.count > 0);
    }

    public boolean pressing() {
        return this.button >= 0;
    }

    public int source() {
        return this.source;
    }

    public boolean spreadsOver(final int slot) {
        return this.spread.size() > 1 && this.spread.contains(slot);
    }

    public void cancel() {
        this.source = -1;
        this.count = 0;
        this.button = -1;
        this.spread.clear();
        this.loose = null;
        this.pending = null;
        this.base = null;
    }

    public @Nullable ItemSpec carried(final List<ItemSpec> slots) {
        if (this.loose != null) {
            return this.loose.copy();
        }
        if (!this.holding()) {
            return null;
        }
        final ItemSpec spec = slots.get(this.source);
        if (spec == null) {
            return null;
        }
        final ItemSpec copy = spec.copy();
        copy.setCount(this.count);
        return copy;
    }

    public @Nullable ItemSpec shown(final int slot, @Nullable final ItemSpec spec) {
        if (this.pending != null) {
            return slot < this.pending.size() ? this.pending.get(slot) : spec;
        }
        if (spec == null || !this.holding() || slot != this.source) {
            return spec;
        }
        final int left = spec.getCount() - this.count;
        if (left <= 0) {
            return null;
        }
        final ItemSpec copy = spec.copy();
        copy.setCount(left);
        return copy;
    }

    public void validate(final List<ItemSpec> slots) {
        if (this.pending != null) {
            if (this.base == null || changed(this.base, slots)) {
                this.cancel();
            }
            return;
        }
        if (this.source < 0) {
            return;
        }
        final ItemSpec spec = this.source < slots.size() ? slots.get(this.source) : null;
        if (spec == null) {
            this.cancel();
            return;
        }
        this.count = Math.min(this.count, spec.getCount());
        if (this.count <= 0) {
            this.cancel();
        }
    }

    public boolean pickHalf(final List<ItemSpec> slots, final int slot) {
        final ItemSpec spec = slots.get(slot);
        if (spec == null) {
            return false;
        }
        this.source = slot;
        this.count = (spec.getCount() + 1) / 2;
        return true;
    }

    public void press(final int button, final int slot) {
        this.button = button;
        this.spread.clear();
        this.spread.add(slot);
    }

    public void dragOver(final List<ItemSpec> slots, final int slot, final Rules rules) {
        if (!this.pressing() || slot < 0 || this.spread.contains(slot) || this.spread.size() >= this.carriedCount()) {
            return;
        }
        if (this.roomIn(this.pending != null ? this.pending : slots, slot, rules) > 0) {
            this.spread.add(slot);
        }
    }

    public @Nullable List<ItemSpec> release(final List<ItemSpec> current, final Rules rules) {
        if (!this.pressing()) {
            return null;
        }
        final int pressed = this.button;
        final List<Integer> targets = new ArrayList<>(this.spread);
        this.button = -1;
        this.spread.clear();
        if (!this.holding()) {
            return null;
        }
        final List<ItemSpec> slots = copyOf(this.pending != null ? this.pending : current);
        if (targets.size() > 1) {
            this.distribute(slots, targets, pressed, rules);
        } else if (!targets.isEmpty()) {
            this.click(slots, targets.get(0), pressed, rules);
        }
        return this.settle(current, slots);
    }

    public @Nullable List<ItemSpec> discard(final List<ItemSpec> current) {
        if (!this.holding()) {
            return null;
        }
        final List<ItemSpec> slots = copyOf(this.pending != null ? this.pending : current);
        if (this.loose == null) {
            final ItemSpec spec = slots.get(this.source);
            if (spec != null) {
                final int left = spec.getCount() - this.count;
                if (left <= 0) {
                    slots.set(this.source, null);
                } else {
                    spec.setCount(left);
                }
            }
        }
        this.cancel();
        return changed(current, slots) ? slots : null;
    }

    private @Nullable List<ItemSpec> settle(final List<ItemSpec> current, final List<ItemSpec> slots) {
        if (this.loose != null) {
            if (this.base == null) {
                this.base = copyOf(current);
            }
            this.pending = slots;
            return null;
        }
        this.pending = null;
        this.base = null;
        if (this.count <= 0) {
            this.source = -1;
            this.count = 0;
        }
        return changed(current, slots) ? slots : null;
    }

    private int carriedCount() {
        return this.loose != null ? this.loose.getCount() : this.count;
    }

    private void distribute(final List<ItemSpec> slots, final List<Integer> targets, final int pressed, final Rules rules) {
        final List<Integer> open = new ArrayList<>();
        for (final int target : targets) {
            if (this.roomIn(slots, target, rules) > 0) {
                open.add(target);
            }
        }
        if (open.isEmpty()) {
            return;
        }
        final int each = pressed == InputConstants.MOUSE_BUTTON_RIGHT ? 1 : Math.max(1, this.carriedCount() / open.size());
        for (final int target : open) {
            if (this.carriedCount() <= 0) {
                break;
            }
            this.move(slots, target, Math.min(each, Math.min(this.carriedCount(), this.roomIn(slots, target, rules))));
        }
    }

    private void click(final List<ItemSpec> slots, final int target, final int pressed, final Rules rules) {
        final boolean one = pressed == InputConstants.MOUSE_BUTTON_RIGHT;
        if (this.loose != null) {
            this.clickLoose(slots, target, one, rules);
            return;
        }
        if (target == this.source) {
            if (one) {
                this.count--;
            } else {
                this.cancel();
            }
            return;
        }
        final ItemSpec moving = slots.get(this.source);
        final ItemSpec there = slots.get(target);
        if (moving == null) {
            this.cancel();
            return;
        }
        if (there == null || moving.canStackWith(there)) {
            this.move(slots, target, Math.min(one ? 1 : this.count, this.roomIn(slots, target, rules)));
            return;
        }
        if (!rules.accepts(target, moving) || this.count > rules.limit(target, moving)) {
            return;
        }
        if (this.count < moving.getCount()) {
            final ItemSpec placed = moving.copy();
            placed.setCount(this.count);
            moving.setCount(moving.getCount() - this.count);
            slots.set(target, placed);
            this.loose = there;
            this.source = -1;
            this.count = 0;
            return;
        }
        if (rules.accepts(this.source, there) && there.getCount() <= rules.limit(this.source, there)) {
            slots.set(target, moving);
            slots.set(this.source, there);
            this.count = there.getCount();
        }
    }

    private void clickLoose(final List<ItemSpec> slots, final int target, final boolean one, final Rules rules) {
        final ItemSpec there = slots.get(target);
        if (!rules.accepts(target, this.loose)) {
            return;
        }
        if (there == null || this.loose.canStackWith(there)) {
            this.move(slots, target, Math.min(one ? 1 : this.loose.getCount(), this.roomIn(slots, target, rules)));
            return;
        }
        if (this.loose.getCount() <= rules.limit(target, this.loose)) {
            slots.set(target, this.loose);
            this.loose = there;
        }
    }

    private int roomIn(final List<ItemSpec> slots, final int target, final Rules rules) {
        if (this.loose == null && (target == this.source || this.source < 0)) {
            return 0;
        }
        final ItemSpec moving = this.loose != null ? this.loose : slots.get(this.source);
        if (moving == null || !rules.accepts(target, moving)) {
            return 0;
        }
        final ItemSpec there = slots.get(target);
        if (there == null) {
            return rules.limit(target, moving);
        }
        return moving.canStackWith(there) ? Math.max(0, rules.limit(target, there) - there.getCount()) : 0;
    }

    private void move(final List<ItemSpec> slots, final int target, final int amount) {
        if (amount <= 0) {
            return;
        }
        final ItemSpec moving = this.loose != null ? this.loose : slots.get(this.source);
        final ItemSpec there = slots.get(target);
        if (there == null) {
            final ItemSpec placed = moving.copy();
            placed.setCount(amount);
            slots.set(target, placed);
        } else {
            there.setCount(there.getCount() + amount);
        }
        if (this.loose != null) {
            if (this.loose.getCount() - amount <= 0) {
                this.loose = null;
            } else {
                this.loose.setCount(this.loose.getCount() - amount);
            }
            return;
        }
        if (moving.getCount() - amount <= 0) {
            slots.set(this.source, null);
        } else {
            moving.setCount(moving.getCount() - amount);
        }
        this.count -= amount;
    }

    private static List<ItemSpec> copyOf(final List<ItemSpec> slots) {
        final List<ItemSpec> out = new ArrayList<>(slots.size());
        for (final ItemSpec spec : slots) {
            out.add(spec == null ? null : spec.copy());
        }
        return out;
    }

    private static boolean changed(final List<ItemSpec> before, final List<ItemSpec> after) {
        if (before.size() != after.size()) {
            return true;
        }
        for (int i = 0; i < before.size(); i++) {
            final ItemSpec a = before.get(i);
            final ItemSpec b = after.get(i);
            if (a == null ? b != null : !a.equals(b)) {
                return true;
            }
        }
        return false;
    }
}
