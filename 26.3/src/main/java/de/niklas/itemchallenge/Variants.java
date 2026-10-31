package de.niklas.itemchallenge;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Some items are a single registry entry with many in-game variants.
 * This class expands those into one challenge entry per variant.
 * Entry format: "minecraft:potion#minecraft:swiftness"
 */
public final class Variants {

    public static final String SEP = "#";

    private static final Set<String> POTION_ITEMS = Set.of(
            "minecraft:potion",
            "minecraft:splash_potion",
            "minecraft:lingering_potion",
            "minecraft:tipped_arrow"
    );

    private static final String ENCHANTED_BOOK = "minecraft:enchanted_book";

    private Variants() {}

    public static String baseOf(String entryId) {
        int i = entryId.indexOf(SEP);
        return i < 0 ? entryId : entryId.substring(0, i);
    }

    public static String variantOf(String entryId) {
        int i = entryId.indexOf(SEP);
        return i < 0 ? null : entryId.substring(i + 1);
    }

    /** One entry for plain items, many for variant items. */
    public static List<String> expand(String itemId, Minecraft client) {
        List<String> out = new ArrayList<>();

        if (POTION_ITEMS.contains(itemId)) {
            for (Potion p : BuiltInRegistries.POTION) {
                out.add(itemId + SEP + BuiltInRegistries.POTION.getKey(p));
            }
        } else if (ENCHANTED_BOOK.equals(itemId)) {
            try {
                HolderLookup.RegistryLookup<Enchantment> lookup =
                        client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                lookup.listElements().forEach(ref -> out.add(itemId + SEP + ref.key().identifier()));
            } catch (Throwable t) {
                System.err.println("[ItemChallenge] Enchantment lookup failed: " + t);
            }
        }

        if (out.isEmpty()) {
            out.add(itemId);
        } else {
            out.sort(String::compareTo); // deterministic, so seeds stay reproducible
        }
        return out;
    }

    /** Builds the display stack, with components applied for variants. */
    public static ItemStack buildStack(String entryId, Item baseItem) {
        ItemStack stack = new ItemStack(baseItem);
        String base = baseOf(entryId);
        String variant = variantOf(entryId);
        if (variant == null) return stack;

        if (POTION_ITEMS.contains(base)) {
            for (Potion p : BuiltInRegistries.POTION) {
                if (BuiltInRegistries.POTION.getKey(p).toString().equals(variant)) {
                    stack.set(DataComponents.POTION_CONTENTS,
                            new PotionContents(BuiltInRegistries.POTION.wrapAsHolder(p)));
                    break;
                }
            }
        }
        // Enchanted books stay plain; the name is built separately.
        return stack;
    }

    /** Checks whether an inventory stack satisfies this entry. */
    public static boolean matches(ItemStack stack, String entryId, String stackItemId) {
        String base = baseOf(entryId);
        if (!base.equals(stackItemId)) return false;

        String variant = variantOf(entryId);
        if (variant == null) return true;

        if (POTION_ITEMS.contains(base)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) return false;
            return contents.potion()
                    .flatMap(Holder::unwrapKey)
                    .map(key -> key.identifier().toString().equals(variant))
                    .orElse(false);
        }

        if (ENCHANTED_BOOK.equals(base)) {
            ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
            if (stored == null) return false;
            for (Holder<Enchantment> holder : stored.keySet()) {
                if (holder.unwrapKey()
                        .map(key -> key.identifier().toString().equals(variant))
                        .orElse(false)) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    /** Display name. Potions carry their own, books get a readable suffix. */
    public static String displayName(String entryId, ItemStack stack) {
        String variant = variantOf(entryId);
        if (variant != null && ENCHANTED_BOOK.equals(baseOf(entryId))) {
            return "Enchanted Book: " + prettify(variant);
        }
        return stack.isEmpty() ? entryId : stack.getHoverName().getString();
    }

    /** "minecraft:fire_protection" -> "Fire Protection" */
    public static String prettify(String id) {
        String s = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] parts = s.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}