package de.niklas.itemchallenge;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

public final class ChallengeHud {

    private static final float NAME_SCALE = 1.6f;

    private ChallengeHud() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ChallengeState.isActive() || ChallengeState.isHidden()) return;

        Font font = mc.font;
        int centerX = g.guiWidth() / 2;

        if (ChallengeState.isFinished()) {
            renderFinished(g, font, centerX, g.guiHeight());
            return;
        }

        ItemStack stack = ChallengeState.currentStack();
        if (stack.isEmpty()) return;

        String pos = String.valueOf(ChallengeState.currentNumber());
        String tot = String.valueOf(ChallengeState.total());
        String name = stack.getHoverName().getString();

        boolean warn = ChallengeState.currentIsUnobtainable();
        String sub;
        if (ChallengeState.isPaused()) {
            sub = "pausiert - /challenge resume";
        } else if (warn) {
            sub = "nicht in Survival erreichbar - /challenge skip";
        } else {
            sub = ChallengeState.currentId();
            if (ChallengeState.skippedCount() > 0) {
                sub = sub + "   -   " + ChallengeState.skippedCount() + " übersprungen";
            }
        }
        int subColor = (warn || ChallengeState.isPaused()) ? 0xFFEF9F27 : 0xFF9A9A9A;
        int mainColor = ChallengeState.colorArgb();

        int counterW = Math.max(font.width(pos), Math.max(font.width("von"), font.width(tot)));
        int rightW = Math.max((int) (font.width(name) * NAME_SCALE), font.width(sub));
        int boxW = 6 + counterW + 6 + 16 + 8 + rightW + 6;
        int boxH = 36;
        int x0 = centerX - boxW / 2;
        int y0 = 2;

        if (ChallengeState.hasBackground()) {
            g.fill(x0, y0, x0 + boxW, y0 + boxH, 0x99000000);
        }

        int colX = x0 + 6 + counterW / 2;
        g.centeredText(font, pos, colX, y0 + 4, mainColor);
        g.centeredText(font, "von", colX, y0 + 14, 0xFF9A9A9A);
        g.centeredText(font, tot, colX, y0 + 24, mainColor);

        int iconX = x0 + 6 + counterW + 6;
        g.item(stack, iconX, y0 + (boxH - 16) / 2);

        int textX = iconX + 16 + 8;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(textX, y0 + 5);
        pose.scale(NAME_SCALE, NAME_SCALE);
        g.text(font, name, 0, 0, mainColor, true);
        pose.popMatrix();

        g.text(font, sub, textX, y0 + 23, subColor, true);
    }

    private static void renderFinished(GuiGraphicsExtractor g, Font font, int centerX, int screenH) {
        long since = System.currentTimeMillis() - ChallengeState.finishedAtMs();
        String stats = ChallengeState.collectedCount() + " gesammelt   -   "
                + ChallengeState.skippedCount() + " übersprungen   -   Seed "
                + ChallengeState.seed();

        if (since < 12_000L) {
            String title = "CHALLENGE ABGESCHLOSSEN";
            float t = since / 1000.0f;
            int color = rainbow((t * 0.35f) % 1.0f);
            float scale = 3.0f + (float) Math.sin(t * 2.0) * 0.12f;

            int cy = screenH / 3;
            Matrix3x2fStack pose = g.pose();
            pose.pushMatrix();
            pose.translate(centerX, cy);
            pose.scale(scale, scale);
            g.text(font, title, -font.width(title) / 2, 0, color, true);
            pose.popMatrix();

            g.centeredText(font, stats, centerX, cy + 40, 0xFFFFFFFF);
        } else {
            if (ChallengeState.hasBackground()) {
                int w = font.width(stats);
                g.fill(centerX - w / 2 - 6, 2, centerX + w / 2 + 6, 16, 0x99000000);
            }
            g.centeredText(font, stats, centerX, 5, 0xFF5DCAA5);
        }
    }

    private static int rainbow(float hue) {
        float h = (hue % 1.0f) * 6.0f;
        int i = (int) h;
        float f = h - i;
        int v = 255;
        int p = 40;
        int q = (int) (255 * (1.0f - f * 0.85f));
        int t = (int) (255 * (0.15f + f * 0.85f));
        int r, gg, b;
        switch (i) {
            case 0 -> { r = v; gg = t; b = p; }
            case 1 -> { r = q; gg = v; b = p; }
            case 2 -> { r = p; gg = v; b = t; }
            case 3 -> { r = p; gg = q; b = v; }
            case 4 -> { r = t; gg = p; b = v; }
            default -> { r = v; gg = p; b = q; }
        }
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }
}