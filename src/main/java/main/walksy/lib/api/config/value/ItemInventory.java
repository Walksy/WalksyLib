package main.walksy.lib.api.config.value;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ItemInventory implements CopyableValue<ItemInventory> {
    public static final int HOTBAR_START = 0;
    public static final int HOTBAR_SIZE = 9;
    public static final int STORAGE_START = 9;
    public static final int STORAGE_ROWS = 3;
    public static final int STORAGE_SIZE = STORAGE_ROWS * HOTBAR_SIZE;
    public static final int ARMOR_START = 36;
    public static final int ARMOR_SIZE = 4;
    public static final int OFFHAND = 40;
    public static final int SIZE = 41;
    public static final int[] ARMOR_ORDER = {ARMOR_START + 3, ARMOR_START + 2, ARMOR_START + 1, ARMOR_START};
    private static final EquipmentSlot[] EQUIPMENT = { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
    private final ItemSpec[] slots;

    public ItemInventory() {
        this.slots = new ItemSpec[SIZE];
    }

    private ItemInventory(final ItemSpec[] slots) {
        this.slots = new ItemSpec[SIZE];
        for (int i = 0; i < SIZE && i < slots.length; i++) {
            this.slots[i] = slots[i] == null ? null : slots[i].copy();
        }
    }

    @Override
    public ItemInventory copy() {
        return new ItemInventory(this.slots);
    }

    public static boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < SIZE;
    }

    public ItemSpec get(final int slot) {
        return isValidSlot(slot) ? this.slots[slot] : null;
    }

    public void set(final int slot, final ItemSpec spec) {
        if (isValidSlot(slot)) {
            this.slots[slot] = spec;
        }
    }

    public void clear(final int slot) {
        set(slot, null);
    }

    public void clearAll() {
        Arrays.fill(this.slots, null);
    }

    public void swap(final int from, final int to) {
        if (!isValidSlot(from) || !isValidSlot(to) || from == to) {
            return;
        }
        final ItemSpec temp = this.slots[to];
        this.slots[to] = this.slots[from];
        this.slots[from] = temp;
    }

    public void drop(final int from, final int to) {
        if (isValidSlot(from) && isValidSlot(to)) {
            ItemSpec.drop(Arrays.asList(this.slots), from, to);
        }
    }

    public int filledCount() {
        int count = 0;
        for (final ItemSpec spec : this.slots) {
            if (spec != null) {
                count++;
            }
        }
        return count;
    }

    public boolean isEmpty() {
        return filledCount() == 0;
    }

    public List<ItemSpec> contents() {
        final List<ItemSpec> out = new ArrayList<>();
        for (final ItemSpec spec : this.slots) {
            if (spec != null) {
                out.add(spec);
            }
        }
        return out;
    }

    public static EquipmentSlot equipmentSlotFor(final int slot) {
        if (slot == OFFHAND) {
            return EquipmentSlot.OFFHAND;
        }
        if (slot >= ARMOR_START && slot < ARMOR_START + ARMOR_SIZE) {
            return EQUIPMENT[slot - ARMOR_START];
        }
        return null;
    }

    public static boolean accepts(final int slot, final String itemId) {
        final EquipmentSlot equipment = equipmentSlotFor(slot);
        if (equipment == null || equipment == EquipmentSlot.OFFHAND) {
            return true;
        }
        final ItemSpec spec = new ItemSpec(itemId);
        if (!spec.isResolvable()) {
            return false;
        }
        final ItemStack stack = spec.toItemStack();
        if (stack.isEmpty()) {
            return true;
        }
        final Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == equipment;
    }

    public ItemStack stackAt(final int slot) {
        final ItemSpec spec = get(slot);
        return spec == null ? ItemStack.EMPTY : spec.toItemStack();
    }

    public ItemStack[] toStacks() {
        final ItemStack[] stacks = new ItemStack[SIZE];
        for (int i = 0; i < SIZE; i++) {
            stacks[i] = stackAt(i);
        }
        return stacks;
    }

    public static ItemInventory captureFrom(final Player player) {
        if (player == null) {
            return null;
        }
        final ItemInventory captured = new ItemInventory();
        final List<ItemStack> items = player.getInventory().getNonEquipmentItems();
        for (int slot = 0; slot < items.size() && slot < ARMOR_START; slot++) {
            captured.set(slot, ItemSpec.from(items.get(slot)));
        }
        for (int i = 0; i < ARMOR_SIZE; i++) {
            captured.set(ARMOR_START + i, ItemSpec.from(player.getItemBySlot(EQUIPMENT[i])));
        }
        captured.set(OFFHAND, ItemSpec.from(player.getItemBySlot(EquipmentSlot.OFFHAND)));
        return captured;
    }

    public static ItemInventory captureFromLocalPlayer() {
        final Minecraft client = Minecraft.getInstance();
        return client == null ? null : captureFrom(client.player);
    }

    public static ItemInventory of(final Object... slotThenSpec) {
        final ItemInventory inventory = new ItemInventory();
        for (int i = 0; i + 1 < slotThenSpec.length; i += 2) {
            inventory.set((Integer) slotThenSpec[i], (ItemSpec) slotThenSpec[i + 1]);
        }
        return inventory;
    }

    @Override
    public boolean equals(final Object other) {
        return this == other || (other instanceof final ItemInventory inventory && Arrays.equals(this.slots, inventory.slots));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.slots);
    }

    @Override
    public String toString() {
        return "ItemInventory[" + filledCount() + " filled]";
    }
}
