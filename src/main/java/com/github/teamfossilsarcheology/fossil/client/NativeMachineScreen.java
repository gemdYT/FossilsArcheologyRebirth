package com.github.teamfossilsarcheology.fossil.client;

import com.github.teamfossilsarcheology.fossil.NativeMachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** One clear native layout, with machine-specific recipes, labels and synchronized progress. */
public final class NativeMachineScreen extends AbstractContainerScreen<NativeMachineMenu> {
    public NativeMachineScreen(NativeMachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 166); }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xff292c31);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xffd7c8a7);
        for (var slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xff786d59);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xffb5a88f);
        }
        if (!menu.kind.equals("feeder")) {
            graphics.fill(x + 103, y + 40, x + 123, y + 48, 0xff786d59);
            graphics.fill(x + 103, y + 40, x + 103 + menu.progress() / 5, y + 48, 0xff5e9c63);
        }
        String fuel = menu.kind.equals("culture_vat") ? "Bio-Goo" : menu.kind.equals("worktable") ? "Repair" : menu.kind.equals("feeder") ? "Food" : "Unused";
        graphics.text(font, menu.kind.equals("feeder") ? "Food" : "Input", x + 38, y + 24, 0xff403a30, false);
        graphics.text(font, fuel, x + 72, y + 24, 0xff403a30, false);
        if (!menu.kind.equals("feeder")) graphics.text(font, "Output", x + 126, y + 24, 0xff403a30, false);
        String hint = switch (menu.kind) {
            case "analyzer" -> "Fossils / relics → DNA / artifacts";
            case "culture_vat" -> "DNA + Bio-Goo → revival item";
            case "sifter" -> "Sand / gravel → fossils / relics";
            case "worktable" -> "Artifact + iron / shard → restored";
            default -> "Feeds animals within 8 blocks";
        };
        graphics.text(font, hint, x + 8, y + 65, 0xff403a30, false);
        if (!menu.kind.equals("feeder")) graphics.text(font, "Power " + menu.energy() + "/10000 · optional", x + 8, y + 74, 0xff403a30, false);
    }
}
