package de.niklas.itemchallenge;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class OverviewScreen extends Screen {

    private static final int COLS = 18;
    private static final int CELL = 20;

    private record Section(String title, int color, List<String> ids) {}

    private final List<Section> sections = new ArrayList<>();
    private int scroll = 0;
    private int contentHeight = 0;

    public OverviewScreen() {
        super(Component.literal("Item Challenge"));

        List<String> done = ChallengeState.collectedIds();
        List<String> skipped = ChallengeState.skippedIds();
        List<String> open = ChallengeState.openIdsAlphabetical();

        if (!done.isEmpty()) {
            sections.add(new Section("Collected (" + done.size() + ")", 0xFF5DCAA5, done));
        }
        if (!skipped.isEmpty()) {
            sections.add(new Section("Skipped (" + skipped.size() + ")", 0xFFEF9F27, skipped));
        }
        sections.add(new Section("Remaining (" + open.size() + ") - alphabetical", 0xFFAAAAAA, open));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        int gridW = COLS * CELL;
        int left = (this.width - gridW) / 2;
        int top = 48;
        int bottom = this.height - 16;

        g.centeredText(this.font, "Item Challenge", this.width / 2, 12, 0xFFFFFFFF);
        String stats = ChallengeState.collectedCount() + " collected   -   "
                + ChallengeState.skippedCount() + " skipped   -   "
                + ChallengeState.openCount() + " remaining";
        g.centeredText(this.font, stats, this.width / 2, 24, 0xFFAAAAAA);

        g.fill(left, 38, left + gridW, 42, 0x55FFFFFF);
        int filled = ChallengeState.total() == 0 ? 0
                : (int) ((long) gridW * ChallengeState.collectedCount() / ChallengeState.total());
        g.fill(left, 38, left + filled, 42, 0xFF5DCAA5);

        g.enableScissor(left - 4, top, left + gridW + 4, bottom);

        int y = top - scroll;
        ItemStack hovered = null;

        for (Section s : sections) {
            if (y + 10 > top && y < bottom) {
                g.text(this.font, s.title(), left, y, s.color(), true);
            }
            y += 14;

            int col = 0;
            for (String id : s.ids()) {
                int cx = left + col * CELL;
                if (y + CELL > top && y < bottom) {
                    g.fill(cx, y, cx + 18, y + 18, 0x33FFFFFF);
                    ItemStack stack = ChallengeState.stackOf(id);
                    g.item(stack, cx + 1, y + 1);
                    if (mouseX >= cx && mouseX < cx + 18 && mouseY >= y && mouseY < y + 18
                            && mouseY >= top && mouseY < bottom) {
                        g.fill(cx, y, cx + 18, y + 18, 0x55FFFFFF);
                        hovered = stack;
                    }
                }
                col++;
                if (col >= COLS) {
                    col = 0;
                    y += CELL;
                }
            }
            if (col != 0) y += CELL;
            y += 10;
        }

        contentHeight = y + scroll - top;
        g.disableScissor();

        g.centeredText(this.font, "Scroll with the mouse wheel   -   Esc to close",
                this.width / 2, this.height - 11, 0xFF808080);

        if (hovered != null && !hovered.isEmpty()) {
            g.setTooltipForNextFrame(this.font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int visible = (this.height - 16) - 48;
        int max = Math.max(0, contentHeight - visible);
        scroll = Math.max(0, Math.min(max, scroll - (int) (scrollY * 24)));
        return true;
    }
}