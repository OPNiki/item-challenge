package de.niklas.itemchallenge;

import java.util.Set;

/** Hint list only — nothing is blocked, the HUD line just turns orange. */
public final class Unobtainable {

    private static final Set<String> IDS = Set.of(
            "minecraft:barrier",
            "minecraft:light",
            "minecraft:structure_block",
            "minecraft:structure_void",
            "minecraft:jigsaw",
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:command_block_minecart",
            "minecraft:debug_stick",
            "minecraft:knowledge_book",
            "minecraft:bedrock",
            "minecraft:end_portal_frame",
            "minecraft:spawner",
            "minecraft:trial_spawner",
            "minecraft:vault",
            "minecraft:budding_amethyst",
            "minecraft:reinforced_deepslate",
            "minecraft:petrified_oak_slab",
            "minecraft:farmland",
            "minecraft:dirt_path",
            "minecraft:chorus_plant",
            "minecraft:suspicious_sand",
            "minecraft:suspicious_gravel",
            "minecraft:player_head"
    );

    /** Potion types that cannot be brewed in survival. */
    private static final Set<String> POTION_VARIANTS = Set.of(
            "minecraft:luck",
            "minecraft:decay"
    );

    private Unobtainable() {}

    public static boolean is(String entryId) {
        if (entryId == null) return false;

        String base = Variants.baseOf(entryId);
        String variant = Variants.variantOf(entryId);

        if (variant != null && POTION_VARIANTS.contains(variant)) return true;
        if (base.endsWith("_spawn_egg")) return true;
        return IDS.contains(base);
    }
}