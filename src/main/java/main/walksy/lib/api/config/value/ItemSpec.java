package main.walksy.lib.api.config.value;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.platform.WLRegistries;
import main.walksy.lib.core.utils.LegacyText;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ItemSpec implements CopyableValue<ItemSpec> {
    public static final int MAX_COUNT = 64;
    public static final int MAX_LORE_LINES = ItemLore.MAX_LINES;
    public static final int MAX_ENCHANT_LEVEL = 255;
    private String itemId;
    private int count;
    private int damage;
    private boolean unbreakable;
    private String customName;
    private final List<String> lore;
    private final Map<String, Integer> enchantments;
    private String extraNbt;
    public static final int CONTAINER_SIZE = 27;
    private static final String CONTAINER_KEY = "minecraft:container";
    private static final String CONTAINER_SLOT = "slot";
    private static final String CONTAINER_ITEM = "item";
    private static final String AIR_ID = "minecraft:air";
    private static final List<String> STRUCTURED_COMPONENTS = List.of("minecraft:damage", "minecraft:unbreakable", "minecraft:custom_name", "minecraft:lore", "minecraft:enchantments");
    private static boolean loggedEnchantmentFailure;

    public ItemSpec(final String itemId) {
        this(itemId, 1);
    }

    public ItemSpec(final String itemId, final int count) {
        this.itemId = normalizeId(itemId);
        this.count = Math.max(1, count);
        this.customName = "";
        this.lore = new ArrayList<>();
        this.enchantments = new LinkedHashMap<>();
        this.extraNbt = "";
    }

    private ItemSpec(final ItemSpec other) {
        this.itemId = other.itemId;
        this.count = other.count;
        this.damage = other.damage;
        this.unbreakable = other.unbreakable;
        this.customName = other.customName;
        this.lore = new ArrayList<>(other.lore);
        this.enchantments = new LinkedHashMap<>(other.enchantments);
        this.extraNbt = other.extraNbt;
    }

    @Override
    public ItemSpec copy() {
        return new ItemSpec(this);
    }

    public static String normalizeId(final String raw) {
        if (raw == null || raw.isBlank()) {
            return "minecraft:air";
        }
        final String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        return trimmed.indexOf(':') >= 0 ? trimmed : "minecraft:" + trimmed;
    }

    public String getItemId() {
        return this.itemId;
    }

    public void setItemId(final String itemId) {
        this.itemId = normalizeId(itemId);
        clampCountToStack();
    }

    private Holder.Reference<Item> resolveHolder() {
        final Identifier id = Identifier.tryParse(this.itemId);
        return id == null ? null : BuiltInRegistries.ITEM.get(id).orElse(null);
    }

    public Item resolveItem() {
        final Holder.Reference<Item> holder = resolveHolder();
        return holder == null ? null : holder.value();
    }

    public boolean hasBoundComponents() {
        final Holder.Reference<Item> holder = resolveHolder();
        if (holder == null) {
            return false;
        }
        if (WLRegistries.componentsBound(holder)) {
            return true;
        }
        WalksyLib.getInstance().getOfflineRegistries().provider();
        return WLRegistries.componentsBound(holder);
    }

    public boolean isResolvable() {
        final Item item = resolveItem();
        return item != null && item != Items.AIR;
    }

    public String itemDisplayName() {
        final Item item = resolveItem();
        if (item == null) {
            return this.itemId;
        }
        return Component.translatable(item.getDescriptionId()).getString();
    }

    public String displayName() {
        final String stripped = LegacyText.strip(this.customName);
        return stripped.isBlank() ? itemDisplayName() : stripped;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(final int count) {
        this.count = Math.max(1, count);
    }

    public int maxStackSize() {
        final Holder.Reference<Item> holder = resolveHolder();
        if (holder == null || !hasBoundComponents()) {
            return MAX_COUNT;
        }
        final Integer max = WLRegistries.components(holder).get(DataComponents.MAX_STACK_SIZE);
        return max == null ? MAX_COUNT : Math.max(1, max);
    }

    public boolean canStackWith(final ItemSpec other) {
        return other != null && other != this && this.maxStackSize() > 1 && this.damage == other.damage && this.unbreakable == other.unbreakable && this.itemId.equals(other.itemId) && this.extraNbt.equals(other.extraNbt) && this.customName.equals(other.customName) && this.lore.equals(other.lore) && this.enchantments.equals(other.enchantments);
    }

    public int stackOnto(final ItemSpec target) {
        final int moved = Math.max(0, Math.min(this.count, target.maxStackSize() - target.count));
        target.count += moved;
        this.count -= moved;
        return this.count;
    }

    public static void drop(final List<ItemSpec> slots, final int from, final int to) {
        if (from == to || from < 0 || to < 0 || from >= slots.size() || to >= slots.size()) {
            return;
        }
        final ItemSpec moving = slots.get(from);
        final ItemSpec target = slots.get(to);
        if (moving != null && moving.canStackWith(target) && target.getCount() < target.maxStackSize()) {
            slots.set(from, moving.stackOnto(target) == 0 ? null : moving);
            return;
        }
        slots.set(from, target);
        slots.set(to, moving);
    }

    private void clampCountToStack() {
        this.count = Math.max(1, Math.min(maxStackSize(), this.count));
    }

    public int getDamage() {
        return this.damage;
    }

    public void setDamage(final int damage) {
        this.damage = Math.max(0, damage);
    }

    public int maxDamage() {
        final Holder.Reference<Item> holder = resolveHolder();
        if (holder == null || holder.value() == Items.AIR || !hasBoundComponents()) {
            return 0;
        }
        final Integer max = WLRegistries.components(holder).get(DataComponents.MAX_DAMAGE);
        return max == null ? 0 : Math.max(0, max);
    }

    public boolean isDamageable() {
        return maxDamage() > 0;
    }

    public boolean isUnbreakable() {
        return this.unbreakable;
    }

    public void setUnbreakable(final boolean unbreakable) {
        this.unbreakable = unbreakable;
    }

    public String getCustomName() {
        return this.customName;
    }

    public void setCustomName(final String customName) {
        this.customName = customName == null ? "" : customName;
    }

    public boolean hasCustomName() {
        return !this.customName.isBlank();
    }

    public List<String> getLore() {
        return this.lore;
    }

    public void addLoreLine(final String line) {
        if (this.lore.size() < MAX_LORE_LINES) {
            this.lore.add(line == null ? "" : line);
        }
    }

    public void setLoreLine(final int index, final String line) {
        if (index >= 0 && index < this.lore.size()) {
            this.lore.set(index, line == null ? "" : line);
        }
    }

    public void removeLoreLine(final int index) {
        if (index >= 0 && index < this.lore.size()) {
            this.lore.remove(index);
        }
    }

    public Map<String, Integer> getEnchantments() {
        return this.enchantments;
    }

    public int getEnchantmentLevel(final String enchantmentId) {
        final Integer level = this.enchantments.get(normalizeId(enchantmentId));
        return level == null ? 0 : level;
    }

    public void setEnchantment(final String enchantmentId, final int level) {
        final String id = normalizeId(enchantmentId);
        if (level <= 0) {
            this.enchantments.remove(id);
            return;
        }
        this.enchantments.put(id, Math.min(MAX_ENCHANT_LEVEL, level));
    }

    public void removeEnchantment(final String enchantmentId) {
        this.enchantments.remove(normalizeId(enchantmentId));
    }

    public boolean hasEnchantment(final String enchantmentId) {
        return this.enchantments.containsKey(normalizeId(enchantmentId));
    }

    public ItemStack toItemStack() {
        final Item item = resolveItem();
        if (item == null || item == Items.AIR || !hasBoundComponents()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = new ItemStack(item, this.count);
        if (this.damage > 0 && isDamageable()) {
            stack.set(DataComponents.DAMAGE, Math.min(this.damage, maxDamage()));
        }
        if (this.unbreakable) {
            stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        }
        final Component name = LegacyText.toComponent(this.customName);
        if (name != null) {
            stack.set(DataComponents.CUSTOM_NAME, name);
        }
        if (!this.lore.isEmpty()) {
            final List<Component> lines = new ArrayList<>(this.lore.size());
            for (final String line : this.lore) {
                final Component parsed = LegacyText.toComponent(line);
                lines.add(parsed == null ? Component.empty() : parsed);
            }
            stack.set(DataComponents.LORE, new ItemLore(lines));
        }
        applyEnchantments(stack);
        applyExtraNbt(stack);
        return stack;
    }

    private void applyEnchantments(final ItemStack stack) {
        if (this.enchantments.isEmpty()) {
            return;
        }
        final HolderLookup.RegistryLookup<Enchantment> registry = enchantmentRegistry();
        if (registry == null) {
            return;
        }
        final ItemEnchantments.Mutable built = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        boolean any = false;
        for (final Map.Entry<String, Integer> entry : this.enchantments.entrySet()) {
            final Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null) {
                continue;
            }
            final Holder.Reference<Enchantment> holder = enchantmentHolder(registry, entry.getKey());
            if (holder == null) {
                continue;
            }
            built.set(holder, Math.max(1, entry.getValue()));
            any = true;
        }
        if (any) {
            stack.set(DataComponents.ENCHANTMENTS, built.toImmutable());
        }
    }

    public String getExtraNbt() {
        return this.extraNbt;
    }

    public boolean hasExtraNbt() {
        return !this.extraNbt.isBlank();
    }

    public void setExtraNbt(final String snbt) {
        this.extraNbt = snbt == null ? "" : snbt;
    }

    private void applyExtraNbt(final ItemStack stack) {
        if (!hasExtraNbt()) {
            return;
        }
        final HolderLookup.Provider registries = registryAccess();
        if (registries == null) {
            return;
        }
        try {
            final CompoundTag components = TagParser.parseCompoundFully(this.extraNbt);
            final RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
            DataComponentPatch.CODEC.parse(ops, components).result().ifPresent(stack::applyComponents);
        } catch (final CommandSyntaxException | RuntimeException unusable) {
            WalksyLib.getLogger().err("Ignoring unparseable NBT on " + this.itemId + ": " + unusable.getMessage());
        }
    }

    public String toSnbt() {
        final HolderLookup.Provider registries = registryAccess();
        final ItemStack stack = toItemStack();
        if (registries == null || stack.isEmpty()) {
            return null;
        }
        final RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        return ItemStack.CODEC.encodeStart(ops, stack).result().map(tag -> new SnbtPrinterTagVisitor().visit(tag)).orElse(null);
    }

    public String applySnbt(final String snbt) {
        final HolderLookup.Provider registries = registryAccess();
        if (registries == null) {
            return "NBT editing is unavailable right now";
        }
        final CompoundTag tag;
        try {
            tag = TagParser.parseCompoundFully(snbt);
        } catch (final CommandSyntaxException bad) {
            return bad.getMessage();
        }
        final RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        final DataResult<ItemStack> decoded = ItemStack.CODEC.parse(ops, tag);
        if (decoded.isError()) {
            return decoded.error().map(DataResult.Error::message).orElse("Not a valid item");
        }
        final ItemStack stack = decoded.result().orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) {
            return "Not a valid item";
        }
        copyFrom(from(stack));
        final CompoundTag components = tag.getCompound("components").orElse(null);
        this.extraNbt = components == null ? "" : printExtras(components);
        return null;
    }

    private static String printExtras(final CompoundTag components) {
        final CompoundTag extras = components.copy();
        for (final String owned : STRUCTURED_COMPONENTS) {
            extras.remove(owned);
            extras.remove(stripNamespace(owned));
        }
        return extras.isEmpty() ? "" : new SnbtPrinterTagVisitor().visit(extras);
    }

    private static String stripNamespace(final String key) {
        final int colon = key.indexOf(':');
        return colon < 0 ? key : key.substring(colon + 1);
    }

    private void copyFrom(final ItemSpec other) {
        this.itemId = other.itemId;
        this.count = other.count;
        this.damage = other.damage;
        this.unbreakable = other.unbreakable;
        this.customName = other.customName;
        this.lore.clear();
        this.lore.addAll(other.lore);
        this.enchantments.clear();
        this.enchantments.putAll(other.enchantments);
    }

    public static HolderLookup.Provider registryAccess() {
        final Minecraft client = Minecraft.getInstance();
        if (client != null && client.getConnection() != null) {
            try {
                return client.getConnection().registryAccess();
            } catch (final RuntimeException notReady) {
            }
        }
        return WalksyLib.getInstance().getOfflineRegistries().provider();
    }

    public static HolderLookup.RegistryLookup<Enchantment> enchantmentRegistry() {
        final Minecraft client = Minecraft.getInstance();
        if (client != null) {
            if (client.getConnection() != null) {
                final HolderLookup.RegistryLookup<Enchantment> connected = lookup(client.getConnection().registryAccess());
                if (connected != null) {
                    return connected;
                }
            }
            if (client.level != null) {
                final HolderLookup.RegistryLookup<Enchantment> level = lookup(client.level.registryAccess());
                if (level != null) {
                    return level;
                }
            }
        }
        return lookup(WalksyLib.getInstance().getOfflineRegistries().provider());
    }

    private static HolderLookup.RegistryLookup<Enchantment> lookup(final HolderLookup.Provider registries) {
        if (registries == null) {
            return null;
        }
        try {
            return registries.lookup(Registries.ENCHANTMENT).orElse(null);
        } catch (final RuntimeException notReady) {
            if (!loggedEnchantmentFailure) {
                loggedEnchantmentFailure = true;
                WalksyLib.getLogger().err("Enchantment registry unavailable (" + notReady + ") — the item" + " editor's enchantment list will be empty until a world is joined.");
            }
            return null;
        }
    }

    public static String enchantmentDisplayName(final String enchantmentId) {
        final Identifier id = Identifier.tryParse(enchantmentId);
        if (id == null) {
            return enchantmentId;
        }
        final Holder.Reference<Enchantment> holder = enchantmentHolder(enchantmentRegistry(), enchantmentId);
        return holder == null ? ListEntries.humanize(id) : holder.value().description().getString();
    }

    public List<String> applicableEnchantments() {
        final HolderLookup.RegistryLookup<Enchantment> registry = enchantmentRegistry();
        if (registry == null) {
            return List.of();
        }
        final ItemStack stack = toItemStack();
        if (stack.isEmpty()) {
            return List.of();
        }
        final boolean anythingGoes = stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK) || !itemTagsHaveContent();
        final List<Holder.Reference<Enchantment>> applied = appliedHolders(registry);
        final List<String> out = new ArrayList<>();
        for (final Holder.Reference<Enchantment> holder : registry.listElements().toList()) {
            final String key = holder.key().identifier().toString();
            if (this.enchantments.containsKey(key)) {
                out.add(key);
                continue;
            }
            if (!anythingGoes && !supports(holder, stack)) {
                continue;
            }
            if (conflictsWithApplied(holder, applied)) {
                continue;
            }
            out.add(key);
        }
        return out;
    }

    private List<Holder.Reference<Enchantment>> appliedHolders(final HolderLookup.RegistryLookup<Enchantment> registry) {
        final List<Holder.Reference<Enchantment>> holders = new ArrayList<>(this.enchantments.size());
        for (final String applied : this.enchantments.keySet()) {
            final Holder.Reference<Enchantment> holder = enchantmentHolder(registry, applied);
            if (holder != null) {
                holders.add(holder);
            }
        }
        return holders;
    }

    private static boolean conflictsWithApplied(final Holder.Reference<Enchantment> candidate, final List<Holder.Reference<Enchantment>> applied) {
        for (final Holder.Reference<Enchantment> other : applied) {
            if (conflicts(candidate, other)) {
                return true;
            }
        }
        return false;
    }

    private static boolean conflicts(final Holder.Reference<Enchantment> candidate, final Holder.Reference<Enchantment> other) {
        try {
            return !Enchantment.areCompatible(candidate, other);
        } catch (final RuntimeException unbound) {
            return excludes(candidate, other) || excludes(other, candidate);
        }
    }

    private static boolean excludes(final Holder.Reference<Enchantment> a, final Holder.Reference<Enchantment> b) {
        final HolderSet<Enchantment> exclusive = a.value().exclusiveSet();
        if (exclusive == null) {
            return false;
        }
        final Identifier target = b.key().identifier();
        final var direct = exclusive.unwrap().right();
        if (direct.isPresent()) {
            for (final Holder<Enchantment> member : direct.get()) {
                if (member.unwrapKey().map(key -> key.identifier()).filter(target::equals).isPresent()) {
                    return true;
                }
            }
            return false;
        }
        return exclusive.unwrapKey().map(WalksyLib.getInstance().getOfflineRegistries()::enchantmentsInTag).map(ids -> ids.contains(target)).orElse(false);
    }

    private static boolean supports(final Holder.Reference<Enchantment> holder, final ItemStack stack) {
        try {
            return holder.value().isSupportedItem(stack);
        } catch (final RuntimeException unbound) {
            return true;
        }
    }

    private static boolean itemTagsHaveContent() {
        try {
            return BuiltInRegistries.ITEM.getTags().anyMatch(tag -> tag.isBound() && tag.size() > 0);
        } catch (final RuntimeException notReady) {
            return false;
        }
    }

    public String enchantmentUnavailableReason() {
        if (enchantmentRegistry() == null || toItemStack().isEmpty()) {
            return "Enchantments are unavailable right now";
        }
        return "Nothing can be enchanted onto this item";
    }

    private static Holder.Reference<Enchantment> enchantmentHolder(final HolderLookup.RegistryLookup<Enchantment> registry, final String enchantmentId) {
        final Identifier id = registry == null ? null : Identifier.tryParse(enchantmentId);
        if (id == null) {
            return null;
        }
        return registry.get(ResourceKey.create(Registries.ENCHANTMENT, id)).orElse(null);
    }

    public static String romanLevel(final int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(level);
        };
    }

    public static int enchantmentMaxLevel(final String enchantmentId) {
        final HolderLookup.RegistryLookup<Enchantment> registry = enchantmentRegistry();
        final Holder.Reference<Enchantment> holder = enchantmentHolder(registry, enchantmentId);
        return holder == null ? 1 : Math.max(1, holder.value().getMaxLevel());
    }

    public static ItemSpec from(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        final Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        final ItemSpec spec = new ItemSpec(id == null ? "minecraft:air" : id.toString(), stack.getCount());
        spec.setDamage(stack.getDamageValue());
        spec.setUnbreakable(stack.has(DataComponents.UNBREAKABLE));
        final Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name != null) {
            spec.setCustomName(LegacyText.fromComponent(name));
        }
        final ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (final Component line : lore.lines()) {
                spec.addLoreLine(LegacyText.fromComponent(line));
            }
        }
        for (final var entry : stack.getEnchantments().entrySet()) {
            final var key = entry.getKey().unwrapKey();
            if (key.isPresent()) {
                spec.setEnchantment(key.get().identifier().toString(), entry.getIntValue());
            }
        }
        spec.captureExtraComponents(stack);
        return spec;
    }

    public boolean holdsContainer() {
        final ItemStack stack = this.toItemStack();
        return !stack.isEmpty() && stack.getItem().components().has(DataComponents.CONTAINER);
    }

    public List<ItemSpec> containerContents() {
        final List<ItemSpec> contents = new ArrayList<>(Collections.nCopies(CONTAINER_SIZE, null));
        final CompoundTag extras = parseExtras(this.extraNbt);
        if (extras == null) {
            return contents;
        }
        if (!(extras.get(CONTAINER_KEY) instanceof final ListTag entries)) {
            return contents;
        }
        for (int i = 0; i < entries.size(); i++) {
            if (!(entries.get(i) instanceof final CompoundTag entry)) {
                continue;
            }
            final int slot = intValue(entry.get(CONTAINER_SLOT));
            if (slot < 0 || slot >= CONTAINER_SIZE || !(entry.get(CONTAINER_ITEM) instanceof final CompoundTag item)) {
                continue;
            }
            final ItemSpec inner = new ItemSpec(AIR_ID);
            if (inner.applySnbt(new SnbtPrinterTagVisitor().visit(item)) == null) {
                contents.set(slot, inner);
            }
        }
        return contents;
    }

    public void setContainerContents(final List<ItemSpec> contents) {
        final CompoundTag extras = parseExtras(this.extraNbt);
        final CompoundTag root = extras == null ? new CompoundTag() : extras;
        final ListTag entries = new ListTag();
        for (int slot = 0; slot < Math.min(CONTAINER_SIZE, contents.size()); slot++) {
            final ItemSpec inner = contents.get(slot);
            if (inner == null || inner.toItemStack().isEmpty()) {
                continue;
            }
            final String snbt = inner.toSnbt();
            if (snbt == null) {
                continue;
            }
            final CompoundTag item = parseExtras(snbt);
            if (item == null) {
                continue;
            }
            final CompoundTag entry = new CompoundTag();
            entry.putInt(CONTAINER_SLOT, slot);
            entry.put(CONTAINER_ITEM, item);
            entries.add(entry);
        }
        if (entries.isEmpty()) {
            root.remove(CONTAINER_KEY);
        } else {
            root.put(CONTAINER_KEY, entries);
        }
        this.extraNbt = root.isEmpty() ? "" : new SnbtPrinterTagVisitor().visit(root);
    }

    private static int intValue(final Tag tag) {
        if (tag == null) {
            return -1;
        }
        return NbtOps.INSTANCE.getNumberValue(tag).result().map(Number::intValue).orElse(-1);
    }

    private static CompoundTag parseExtras(final String snbt) {
        if (snbt == null || snbt.isBlank()) {
            return null;
        }
        try {
            return TagParser.parseCompoundFully(snbt);
        } catch (final CommandSyntaxException | RuntimeException unusable) {
            return null;
        }
    }

    private void captureExtraComponents(final ItemStack stack) {
        final HolderLookup.Provider registries = registryAccess();
        if (registries == null) {
            return;
        }
        final RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        if (!(ItemStack.CODEC.encodeStart(ops, stack).result().orElse(null) instanceof final CompoundTag tag)) {
            return;
        }
        final CompoundTag components = tag.getCompound("components").orElse(null);
        this.extraNbt = components == null ? "" : printExtras(components);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final ItemSpec spec)) {
            return false;
        }
        return this.count == spec.count && this.damage == spec.damage && this.unbreakable == spec.unbreakable && this.itemId.equals(spec.itemId) && this.extraNbt.equals(spec.extraNbt) && this.customName.equals(spec.customName) && this.lore.equals(spec.lore) && this.enchantments.equals(spec.enchantments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.itemId, this.count, this.damage, this.unbreakable, this.customName, this.lore, this.enchantments, this.extraNbt);
    }

    @Override
    public String toString() {
        return this.count + "x " + this.itemId + (this.enchantments.isEmpty() ? "" : " " + this.enchantments);
    }
}
