package de.niklas.itemchallenge;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public class ItemChallengeClient implements ClientModInitializer {

    public static final String MOD_ID = "itemchallenge";

    /** Screens dürfen nicht mitten im Command geöffnet werden, deshalb ein Tick später. */
    public static boolean openOverviewRequested = false;

    @Override
    public void onInitializeClient() {
        ChallengeCommands.register();

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "challenge"),
                ChallengeHud::render
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ChallengeState.tick(client);
            if (openOverviewRequested && client.player != null) {
                openOverviewRequested = false;
                client.gui.setScreen(new OverviewScreen());
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ChallengeState.load(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ChallengeState.unload());
    }
}