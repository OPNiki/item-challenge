package de.niklas.itemchallenge;

import java.util.Set;

/** Reine Hinweisliste: blockiert nichts, färbt nur die Zeile im HUD orange. */
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

    private Unobtainable() {}

    public static boolean is(String id) {
        if (id == null) return false;
        if (id.endsWith("_spawn_egg")) return true;
        return IDS.contains(id);
    }
}