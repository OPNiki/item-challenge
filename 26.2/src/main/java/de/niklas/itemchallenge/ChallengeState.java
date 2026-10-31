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

    private static Map<String, ItemStack> stackCache = null;
    private static List<String> openCache = null;

    private ChallengeState() {}

    // ---- Pool aus der laufenden Version -----------------------------------

    /** Alle Items der aktuellen Registry, außer Luft. Nichts hartkodiert. */
    private static List<String> allItemIds() {
        List<String> ids = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ids.add(BuiltInRegistries.ITEM.getKey(item).toString());
        }
        return ids;
    }

    private static Map<String, ItemStack> stacks() {
        if (stackCache == null) {
            stackCache = new HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) continue;
                stackCache.put(BuiltInRegistries.ITEM.getKey(item).toString(), new ItemStack(item));
            }
        }
        return stackCache;
    }

    public static ItemStack stackOf(String id) {
        ItemStack s = stacks().get(id);
        return s == null ? ItemStack.EMPTY : s;
    }

    // ---- Steuerung --------------------------------------------------------

    public static void start(long seed) {
        List<String> ids = allItemIds();
        Collections.shuffle(ids, new Random(seed));

        data = new Data();
        data.seed = seed;
        data.order = ids;
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
        message(client, "§e» §f" + nameOf(id) + " §7übersprungen.");
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
        // Falls man das nächste Item schon dabei hat, rutscht die Kette weiter.
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
        message(client, "§6§lChallenge abgeschlossen!");
        message(client, "§7" + data.collected.size() + " gesammelt, "
                + data.skipped.size() + " übersprungen, Seed §f" + data.seed);
        client.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
    }

    private static boolean hasItem(Minecraft client, String id) {
        ItemStack target = stackOf(id);
        if (target.isEmpty()) return false;
        Item item = target.getItem();

        Inventory inv = client.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.getItem() == item) return true;
        }
        for (EquipmentSlot slot : EQUIPMENT) {
            ItemStack s = client.player.getItemBySlot(slot);
            if (!s.isEmpty() && s.getItem() == item) return true;
        }
        return false;
    }

    private static void message(Minecraft client, String text) {
        client.gui.chatListener().handleSystemMessage(Component.literal(text), false);
    }

    // ---- Abfragen ---------------------------------------------------------

    public static boolean isActive()     { return data.active; }
    public static boolean isPaused()      { return data.paused; }
    public static boolean isHidden()      { return data.hidden; }
    public static boolean hasBackground() { return data.background; }
    public static int colorArgb()         { return 0xFF000000 | (data.colorRgb & 0xFFFFFF); }
    public static String colorHex()       { return String.format("%06X", data.colorRgb & 0xFFFFFF); }

    public static void setPaused(boolean v)     { data.paused = v; save(); }
    public static void setHidden(boolean v)     { data.hidden = v; save(); }
    public static void setBackground(boolean v) { data.background = v; save(); }
    public static void setColorRgb(int rgb)     { data.colorRgb = rgb & 0xFFFFFF; save(); }

    /** Schreibt alle Item-IDs der laufenden Version in eine Textdatei. */
    public static Path dumpItems() throws Exception {
        List<String> ids = allItemIds();
        ids.sort(String::compareTo);
        Path out = FabricLoader.getInstance().getConfigDir()
                .resolve("item-challenge").resolve("items.txt");
        Files.createDirectories(out.getParent());
        Files.write(out, ids);
        return out;
    }
    public static boolean isFinished()   { return data.finished; }
    public static long finishedAtMs()    { return data.finishedAtMs; }
    public static long seed()            { return data.seed; }
    public static int total()            { return data.order.size(); }
    public static int collectedCount()   { return data.collected.size(); }
    public static int skippedCount()     { return data.skipped.size(); }
    public static int openCount()        { return Math.max(0, total() - data.index); }
    public static int currentNumber()    { return Math.min(data.index + 1, Math.max(total(), 1)); }

    public static String currentId() {
        if (data.index < 0 || data.index >= data.order.size()) return null;
        return data.order.get(data.index);
    }

    public static ItemStack currentStack() {
        String id = currentId();
        return id == null ? ItemStack.EMPTY : stackOf(id);
    }

    public static boolean currentIsUnobtainable() { return Unobtainable.is(currentId()); }

    public static String nameOf(String id) {
        ItemStack s = stackOf(id);
        return s.isEmpty() ? id : s.getHoverName().getString();
    }

    public static List<String> collectedIds() { return data.collected; }
    public static List<String> skippedIds()   { return data.skipped; }

    /** Alphabetisch, damit die kommende Reihenfolge nicht verraten wird. */
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

    // ---- Persistenz -------------------------------------------------------

    public static void load(Minecraft client) {
        data = new Data();
        openCache = null;
        file = resolveFile(client);
        if (file == null || !Files.exists(file)) return;
        try {
            Data loaded = GSON.fromJson(Files.readString(file), Data.class);
            if (loaded != null) data = loaded;
        } catch (Exception e) {
            System.err.println("[ItemChallenge] Laden fehlgeschlagen: " + e);
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
            System.err.println("[ItemChallenge] Speichern fehlgeschlagen: " + e);
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