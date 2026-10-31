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
                                return say(ctx.getSource(), "§cNo challenge started yet.");
                            }
                            ItemChallengeClient.openOverviewRequested = true;
                            return 1;
                        }))
                        .then(ClientCommands.literal("skip").executes(ctx -> {
                            if (!ChallengeState.skip(Minecraft.getInstance())) {
                                return say(ctx.getSource(), "§cNothing to skip right now.");
                            }
                            return 1;
                        }))
                        .then(ClientCommands.literal("current").executes(ctx -> {
                            String id = ChallengeState.currentId();
                            if (id == null) return say(ctx.getSource(), "§cNo active task.");
                            return say(ctx.getSource(), "§bCurrent: §f" + ChallengeState.nameOf(id)
                                    + " §7(" + ChallengeState.currentNumber() + "/" + ChallengeState.total() + ")");
                        }))
                        .then(ClientCommands.literal("pause").executes(ctx -> {
                            ChallengeState.setPaused(true);
                            return say(ctx.getSource(), "§eChallenge paused - detection is off.");
                        }))
                        .then(ClientCommands.literal("resume").executes(ctx -> {
                            ChallengeState.setPaused(false);
                            return say(ctx.getSource(), "§aChallenge resumed.");
                        }))
                        .then(ClientCommands.literal("hide").executes(ctx -> {
                            ChallengeState.setHidden(true);
                            return say(ctx.getSource(), "§7Display hidden - /challenge show brings it back.");
                        }))
                        .then(ClientCommands.literal("show").executes(ctx -> {
                            ChallengeState.setHidden(false);
                            return say(ctx.getSource(), "§aDisplay shown.");
                        }))
                        .then(ClientCommands.literal("bg")
                                .then(ClientCommands.literal("on").executes(ctx -> {
                                    ChallengeState.setBackground(true);
                                    return say(ctx.getSource(), "§aBackground on.");
                                }))
                                .then(ClientCommands.literal("off").executes(ctx -> {
                                    ChallengeState.setBackground(false);
                                    return say(ctx.getSource(), "§aBackground off.");
                                })))
                        .then(ClientCommands.literal("color")
                                .then(ClientCommands.argument("hex", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String raw = StringArgumentType.getString(ctx, "hex").replace("#", "").trim();
                                            if (raw.length() != 6) {
                                                return say(ctx.getSource(),
                                                        "§cNeed 6 hex digits, e.g. §f/challenge color FF55FF");
                                            }
                                            try {
                                                ChallengeState.setColorRgb(Integer.parseInt(raw, 16));
                                                return say(ctx.getSource(), "§aColor set to §f#" + ChallengeState.colorHex());
                                            } catch (NumberFormatException e) {
                                                return say(ctx.getSource(), "§cNot a valid hex value.");
                                            }
                                        })))
                        .then(ClientCommands.literal("dump").executes(ctx -> {
                            try {
                                java.nio.file.Path p = ChallengeState.dumpItems(Minecraft.getInstance());
                                return say(ctx.getSource(), "§aItem list written to §f" + p);
                            } catch (Exception e) {
                                return say(ctx.getSource(), "§cWriting failed: " + e.getMessage());
                            }
                        }))
                        .then(ClientCommands.literal("reset").executes(ctx -> {
                            ChallengeState.reset();
                            return say(ctx.getSource(), "§cChallenge reset.");
                        }))
                        .executes(ctx -> say(ctx.getSource(),
                                "§b/challenge start [seed] | list | skip | current | pause | resume | hide | show | bg | color | dump | reset"))
                )
        );
    }

    private static int doStart(FabricClientCommandSource source, long seed) {
        ChallengeState.start(Minecraft.getInstance(), seed);
        return say(source, "§aChallenge started: §f" + ChallengeState.total()
                + " entries §7(seed " + seed + ")");
    }

    private static int say(FabricClientCommandSource source, String msg) {
        source.sendFeedback(Component.literal(msg));
        return 1;
    }
}