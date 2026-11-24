package ru.smerk3r.visuals;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VisualsScreen extends Screen {
    private static final int ACCENT = 0xFF62D9FF;
    private static final int PANEL = 0xF0141822;
    private static final int TEXT = 0xFFF0F5FC;
    private static final int MUTED = 0xFF8997AA;

    private static final Map<String, LinkedHashMap<String, Boolean>> MODULES = new LinkedHashMap<>();

    static {
        MODULES.put("HUD", modules(
                "Watermark", "FPS", "CPS", "Coordinates", "Direction", "Keystrokes"
        ));
        MODULES.put("VISUALS", modules(
                "Custom Crosshair", "Hit Color", "Item Glint", "Fullbright"
        ));
        MODULES.put("INTERFACE", modules(
                "Array List", "Notifications", "Compact Chat", "Smooth Animations"
        ));
    }

    private String selectedCategory = "HUD";

    public VisualsScreen() {
        super(Component.literal("Smerk3rVisuals"));
    }

    private static LinkedHashMap<String, Boolean> modules(String... names) {
        LinkedHashMap<String, Boolean> result = new LinkedHashMap<>();
        for (String name : names) result.put(name, false);
        return result;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(520, this.width - 32);
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(18, (this.height - 330) / 2);

        int categoryY = top + 86;
        for (String category : MODULES.keySet()) {
            addRenderableWidget(Button.builder(
                    Component.literal((selectedCategory.equals(category) ? "◆  " : "   ") + category),
                    button -> {
                        selectedCategory = category;
                        rebuildWidgets();
                    }
            ).bounds(left + 16, categoryY, 122, 28).build());
            categoryY += 36;
        }

        int contentX = left + 158;
        int contentWidth = panelWidth - 174;
        int rowY = top + 92;
        for (Map.Entry<String, Boolean> entry : MODULES.get(selectedCategory).entrySet()) {
            String module = entry.getKey();
            addRenderableWidget(Button.builder(
                    Component.literal(module + (entry.getValue() ? "     ON" : "     OFF")),
                    button -> {
                        MODULES.get(selectedCategory).put(module, !MODULES.get(selectedCategory).get(module));
                        rebuildWidgets();
                    }
            ).bounds(contentX, rowY, contentWidth, 30).build());
            rowY += 38;
        }

        addRenderableWidget(Button.builder(
                Component.literal("CLOSE"),
                button -> onClose()
        ).bounds(left + panelWidth - 100, top + 286, 84, 26).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int panelWidth = Math.min(520, this.width - 32);
        int panelHeight = 330;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        graphics.fill(left - 1, top - 1, left + panelWidth + 1, top + panelHeight + 1, 0xAA62D9FF);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left, top, left + panelWidth, top + 3, ACCENT);
        graphics.fill(left + 12, top + 58, left + panelWidth - 12, top + 59, 0x553D536B);
        graphics.fill(left + 145, top + 72, left + 146, top + 272, 0x553D536B);

        graphics.drawString(font, "Smerk3r", left + 18, top + 17, TEXT, true);
        graphics.drawString(font, "VISUALS", left + 75, top + 17, ACCENT, true);
        graphics.drawString(font, "FABRIC CLIENT  /  1.21.11", left + 18, top + 37, MUTED, false);
        graphics.drawString(font, selectedCategory, left + 158, top + 72, ACCENT, true);

        int enabled = 0;
        for (Map<String, Boolean> category : MODULES.values()) {
            for (boolean active : category.values()) if (active) enabled++;
        }
        graphics.drawString(font, "MODULES: " + enabled + " ENABLED", left + 18, top + 294, MUTED, false);
        graphics.drawString(font, "RIGHT SHIFT  •  MENU", left + 18, top + 310, MUTED, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
