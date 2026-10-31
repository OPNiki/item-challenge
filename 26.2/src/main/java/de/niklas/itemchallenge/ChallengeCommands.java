package de.niklas.itemchallenge;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ChallengeCommands {

    private ChallengeCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("challenge")
                        .then(ClientCommands.literal("start")
                                .executes(ctx -> doStart(ctx.getSource(), System.nanoTime()))
                                .then(ClientCommands.argument("seed", LongArgumentType.longArg())
                                        .executes(ctx -> doStart(ctx.getSource(), LongArgumentType.getLong(ctx, "seed")))))
                        .then(ClientCommands.literal("list").executes(ctx -> {
                            if (!ChallengeState.isActive()) {
                                return say(ctx.getSource(), "§cNoch keine Challenge gestartet.");
                            }
                            ItemChallengeClient.openOverviewRequested = true;
                            return 1;
                        }))
                        .then(ClientCommands.literal("skip").executes(ctx -> {
                            if (!ChallengeState.skip(Minecraft.getInstance())) {
                                return say(ctx.getSource(), "§cGerade nichts zum Überspringen da.");
                            }
                            return 1;
                        }))
                        .then(ClientCommands.literal("current").executes(ctx -> {
                            String id = ChallengeState.currentId();
                            if (id == null) return say(ctx.getSource(), "§cKeine aktive Aufgabe.");
                            return say(ctx.getSource(), "§bAktuell: §f" + ChallengeState.nameOf(id)
                                    + " §7(" + ChallengeState.currentNumber() + "/" + ChallengeState.total() + ")");
                        }))
                        .then(ClientCommands.literal("pause").executes(ctx -> {
                            ChallengeState.setPaused(true);
                            return say(ctx.getSource(), "§eChallenge pausiert - es wird nichts mehr erkannt.");
                        }))
                        .then(ClientCommands.literal("resume").executes(ctx -> {
                            ChallengeState.setPaused(false);
                            return say(ctx.getSource(), "§aChallenge läuft weiter.");
                        }))
                        .then(ClientCommands.literal("hide").executes(ctx -> {
                            ChallengeState.setHidden(true);
                            return say(ctx.getSource(), "§7Anzeige ausgeblendet - /challenge show holt sie zurück.");
                        }))
                        .then(ClientCommands.literal("show").executes(ctx -> {
                            ChallengeState.setHidden(false);
                            return say(ctx.getSource(), "§aAnzeige eingeblendet.");
                        }))
                        .then(ClientCommands.literal("bg")
                                .then(ClientCommands.literal("on").executes(ctx -> {
                                    ChallengeState.setBackground(true);
                                    return say(ctx.getSource(), "§aHintergrund an.");
                                }))
                                .then(ClientCommands.literal("off").executes(ctx -> {
                                    ChallengeState.setBackground(false);
                                    return say(ctx.getSource(), "§aHintergrund aus.");
                                })))
                        .then(ClientCommands.literal("color")
                                .then(ClientCommands.argument("hex", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String raw = StringArgumentType.getString(ctx, "hex").replace("#", "").trim();
                                            if (raw.length() != 6) {
                                                return say(ctx.getSource(),
                                                        "§cBitte 6 Hex-Stellen, z. B. §f/challenge color FF55FF");
                                            }
                                            try {
                                                ChallengeState.setColorRgb(Integer.parseInt(raw, 16));
                                                return say(ctx.getSource(), "§aFarbe gesetzt auf §f#" + ChallengeState.colorHex());
                                            } catch (NumberFormatException e) {
                                                return say(ctx.getSource(), "§cKein gültiger Hexwert.");
                                            }
                                        })))
                        .then(ClientCommands.literal("dump").executes(ctx -> {
                            try {
                                java.nio.file.Path p = ChallengeState.dumpItems();
                                return say(ctx.getSource(), "§aItem-Liste geschrieben nach §f" + p);
                            } catch (Exception e) {
                                return say(ctx.getSource(), "§cSchreiben fehlgeschlagen: " + e.getMessage());
                            }
                        }))
                        .then(ClientCommands.literal("reset").executes(ctx -> {
                            ChallengeState.reset();
                            return say(ctx.getSource(), "§cChallenge zurückgesetzt.");
                        }))
                        .executes(ctx -> say(ctx.getSource(),
                                "§b/challenge start [seed] | list | skip | current | pause | resume | hide | show | bg | color | dump | reset"))
                )
        );
    }

    private static int doStart(FabricClientCommandSource source, long seed) {
        ChallengeState.start(seed);
        return say(source, "§aChallenge gestartet: §f" + ChallengeState.total()
                + " Items §7(Seed " + seed + ")");
    }

    private static int say(FabricClientCommandSource source, String msg) {
        source.sendFeedback(Component.literal(msg));
        return 1;
    }
}