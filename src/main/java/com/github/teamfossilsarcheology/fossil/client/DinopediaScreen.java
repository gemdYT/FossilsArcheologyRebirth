package com.github.teamfossilsarcheology.fossil.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DinopediaScreen extends Screen {
    private final String text;
    private int page;
    private java.util.List<net.minecraft.util.FormattedCharSequence> wrapped = java.util.List.of();
    public DinopediaScreen(String text) { super(Component.literal("Dinopedia")); this.text = text; }
    private int linesPerPage() { return Math.max(1, (height - 100) / 12); }
    private java.util.List<net.minecraft.util.FormattedCharSequence> lines() { return wrapped; }
    @Override protected void init() {
        wrapped = font.split(Component.literal(text), Math.max(1, Math.min(440, width - 60)));
        addRenderableWidget(Button.builder(Component.literal("Previous"), button -> page = Math.max(0, page - 1)).bounds(width / 2 - 155, height - 35, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Next"), button -> page = Math.min((lines().size() - 1) / linesPerPage(), page + 1)).bounds(width / 2 + 55, height - 35, 100, 20).build());
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x88000000);
        int x = (width - Math.min(440, width - 60)) / 2;
        graphics.fill(x - 12, 15, width - x + 12, height - 45, 0xffe1d2ae);
        graphics.text(font, "Dinopedia", x, 25, 0xff30271e, false);
        var lines = lines(); int count = linesPerPage(); page = Math.min(page, Math.max(0, (lines.size() - 1) / count));
        for (int i = 0; i < count && page * count + i < lines.size(); i++) graphics.text(font, lines.get(page * count + i), x, 48 + i * 12, 0xff30271e, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
