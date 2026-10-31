package de.niklas.itemchallenge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class ChallengeState {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final EquipmentSlot[] EQUIPMENT = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public static class Data {
        long seed = 0L;
        List<String> order = new ArrayList<>();
        int index = 0;
        List<String> collected = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        boolean active = false;
        boolean finished = false;
        long finishedAtMs = 0L;
        boolean paused = false;
        boolean hidden = false;
        boolean background = true;
        int colorRgb = 0xFFFFFF;
    }

    private static Data data = new Data();
    private static Path file = null;
    private static int cooldown = 0;

    private static Map<String, Item> itemsById = null;
    private static final Map<String, ItemStack> STACK_CACHE = new HashMap<>();
    private static List<String> openCache = null;

    private ChallengeState() {}

    // ---- Item pool --------------------------------------------------------

    private static Map<String, Item> itemsById() {
        if (itemsById == null) {
            itemsById = new HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) continue;
                itemsById.put(BuiltInRegistries.ITEM.getKey(item).toString(), item);
            }
        }
        return itemsById;
    }

    /** Every entry of the running version, variants expanded. Nothing hardcoded. */
    private static List<String> allEntryIds(Minecraft client) {
        List<String> entries = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            entries.addAll(Variants.expand(id, client));
        }
        return entries;
    }

    public static ItemStack stackOf(String entryId) {
        ItemStack cached = STACK_CACHE.get(entryId);
        if (cached != null) return cached;
        Item base = itemsById().get(Variants.baseOf(entryId));
        ItemStack stack = base == null ? ItemStack.EMPTY : Variants.buildStack(entryId, base);
        STACK_CACHE.put(entryId, stack);
        return stack;
    }

    public static String nameOf(String entryId) {
        return Variants.displayName(entryId, stackOf(entryId));
    }

    // ---- Control ----------------------------------------------------------

    public static void start(Minecraft client, long seed) {
        List<String> entries = allEntryIds(client);
        Collections.shuffle(entries, new Random(seed));

        data = new Data();
        data.seed = seed;
        data.order = entries;
        data.active = true;
        openCache = null;
        save();
    }

    public static void reset() {
        data = new Data();
        openCache = null;
        save();
    }

    public static boolean skip(Minecraft client) {
        if (!data.active || data.finished || currentId() == null) return false;
        String id = currentId();
        data.skipped.add(id);
        data.index++;
        openCache = null;
        message(client, "§e» §f" + nameOf(id) + " §7skipped.");
        checkFinished(client);
        save();
        return true;
    }

    // ---- Tick -------------------------------------------------------------

    public static void tick(Minecraft client) {
        if (!data.active || data.finished || data.paused || client.player == null) return;
        if (--cooldown > 0) return;
        cooldown = 5;

        boolean any = false;
        while (!data.finished && currentId() != null && hasItem(client, currentId())) {
            String id = currentId();
            data.collected.add(id);
            data.index++;
            openCache = null;
            any = true;

            message(client, "§a✔ §f" + nameOf(id) + " §7(" + data.collected.size() + "/" + total() + ")");
            client.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));

            checkFinished(client);
        }
        if (any) save();
    }

    private static void checkFinished(Minecraft client) {
        if (data.index < data.order.size()) return;
        data.finished = true;
        data.finishedAtMs = System.currentTimeMillis();
        message(client, "§6§lCHALLENGE COMPLETE!");
        message(client, "§7" + data.collected.size() + " collected, "
                + data.skipped.size() + " skipped, seed §f" + data.seed);
        client.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
    }

    private static boolean hasItem(Minecraft client, String entryId) {
        Inventory inv = client.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (matches(inv.getItem(i), entryId)) return true;
        }
        for (EquipmentSlot slot : EQUIPMENT) {
            if (matches(client.player.getItemBySlot(slot), entryId)) return true;
        }
        return false;
    }

    private static boolean matches(ItemStack stack, String entryId) {
        if (stack.isEmpty()) return false;
        String stackId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return Variants.matches(stack, entryId, stackId);
    }

    private static void message(Minecraft client, String text) {
        client.gui.chatListener().handleSystemMessage(Component.literal(text), false);
    }

    // ---- Queries ----------------------------------------------------------

    public static boolean isActive()      { return data.active; }
    public static boolean isFinished()    { return data.finished; }
    public static boolean isPaused()      { return data.paused; }
    public static boolean isHidden()      { return data.hidden; }
    public static boolean hasBackground() { return data.background; }
    public static long finishedAtMs()     { return data.finishedAtMs; }
    public static long seed()             { return data.seed; }
    public static int total()             { return data.order.size(); }
    public static int collectedCount()    { return data.collected.size(); }
    public static int skippedCount()      { return data.skipped.size(); }
    public static int openCount()         { return Math.max(0, total() - data.index); }
    public static int currentNumber()     { return Math.min(data.index + 1, Math.max(total(), 1)); }

    public static int colorArgb()  { return 0xFF000000 | (data.colorRgb & 0xFFFFFF); }
    public static String colorHex() { return String.format("%06X", data.colorRgb & 0xFFFFFF); }

    public static void setPaused(boolean v)     { data.paused = v; save(); }
    public static void setHidden(boolean v)     { data.hidden = v; save(); }
    public static void setBackground(boolean v) { data.background = v; save(); }
    public static void setColorRgb(int rgb)     { data.colorRgb = rgb & 0xFFFFFF; save(); }

    public static String currentId() {
        if (data.index < 0 || data.index >= data.order.size()) return null;
        return data.order.get(data.index);
    }

    public static ItemStack currentStack() {
        String id = currentId();
        return id == null ? ItemStack.EMPTY : stackOf(id);
    }

    public static boolean currentIsUnobtainable() { return Unobtainable.is(currentId()); }

    public static List<String> collectedIds() { return data.collected; }
    public static List<String> skippedIds()   { return data.skipped; }

    /** Alphabetical, so the upcoming order stays hidden. */
    public static List<String> openIdsAlphabetical() {
        if (openCache != null) return openCache;
        Set<String> gone = new LinkedHashSet<>(data.collected);
        gone.addAll(data.skipped);
        List<String> open = new ArrayList<>();
        for (String id : data.order) if (!gone.contains(id)) open.add(id);
        open.sort((a, b) -> nameOf(a).compareToIgnoreCase(nameOf(b)));
        openCache = open;
        return open;
    }

    /** Writes every entry of the running version to a text file. */
    public static Path dumpItems(Minecraft client) throws Exception {
        List<String> ids = allEntryIds(client);
        ids.sort(String::compareTo);
        Path out = FabricLoader.getInstance().getConfigDir()
                .resolve("item-challenge").resolve("items.txt");
        Files.createDirectories(out.getParent());
        Files.write(out, ids);
        return out;
    }

    // ---- Persistence ------------------------------------------------------

    public static void load(Minecraft client) {
        data = new Data();
        openCache = null;
        STACK_CACHE.clear();
        file = resolveFile(client);
        if (file == null || !Files.exists(file)) return;
        try {
            Data loaded = GSON.fromJson(Files.readString(file), Data.class);
            if (loaded != null) data = loaded;
        } catch (Exception e) {
            System.err.println("[ItemChallenge] Load failed: " + e);
        }
    }

    public static void unload() {
        save();
        file = null;
        openCache = null;
    }

    public static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(data));
        } catch (Exception e) {
            System.err.println("[ItemChallenge] Save failed: " + e);
        }
    }

    private static Path resolveFile(Minecraft client) {
        try {
            IntegratedServer server = client.getSingleplayerServer();
            if (server != null) {
                return server.getWorldPath(LevelResource.ROOT).resolve("item-challenge.json");
            }
        } catch (Throwable ignored) {
        }
        return FabricLoader.getInstance().getConfigDir()
                .resolve("item-challenge").resolve("fallback.json");
    }
}