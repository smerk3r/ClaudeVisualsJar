package dev.claudevisuals;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/** Красивое оформление главного меню: свечение, частицы, анимированный логотип и кнопка меню функций. */
public final class TitleFx {
    private static final float[] RX = new float[100], RY = new float[100], RS = new float[100];
    private static boolean toasted;
    static {
        Random r = new Random(7);
        for (int i = 0; i < 100; i++) { RX[i] = r.nextFloat(); RY[i] = r.nextFloat(); RS[i] = 0.3f + r.nextFloat(); }
    }

    private TitleFx() {}

    public static void attach(Minecraft mc, Screen s) {
        Screens.getButtons(s).add(Button.builder(Component.literal("ClaudeVisuals"), b -> mc.setScreen(new MenuScreen(s)))
            .bounds(6, 58, 110, 20).build());
        ScreenEvents.afterRender(s).register((scr, g, mx, my, pt) -> render(mc, scr, g));
        if (!toasted) {
            toasted = true;
            mc.getToastManager().addToast(SystemToast.multiline(mc, SystemToast.SystemToastId.NARRATOR_TOGGLE,
                Component.literal("ClaudeVisuals загружен"),
                Component.literal(Mods.ALL.size() + " функций · Right Shift — меню")));
        }
    }

    private static void render(Minecraft mc, Screen s, GuiGraphics g) {
        int w = s.width, h = s.height;
        double sec = System.currentTimeMillis() / 1000.0;

        g.fillGradient(0, 0, w, h / 4, 0x90000000, 0x00000000);
        g.fillGradient(0, h * 3 / 4, w, h, 0x00000000, 0xA0000000);
        for (int i = 0; i < 12; i++) {
            int al = (int) (30 * (1 - i / 12.0));
            g.fill(i * 3, 0, i * 3 + 3, h, Hud.a(0xDC7A5A, al));
            g.fill(w - i * 3 - 3, 0, w - i * 3, h, Hud.a(0x7C5CFF, al));
        }
        for (int i = 0; i < 100; i++) {
            float y = (((RY[i] - (float) sec * 0.03f * RS[i]) % 1f) + 1f) % 1f;
            float x = RX[i] + (float) Math.sin(sec * 0.7 + i) * 0.01f;
            int px = (int) (x * w), py = (int) (y * h);
            int al = (int) (40 + 120 * Math.abs(Math.sin(sec * RS[i] + i)));
            int sz = RS[i] > 0.95f ? 2 : 1;
            g.fill(px, py, px + sz, py + sz, Hud.a(i % 3 == 0 ? 0x7C5CFF : 0xDC7A5A, al));
        }
        int sy = (int) ((sec * 40) % h);
        g.fill(0, sy, w, sy + 1, Hud.a(0xDC7A5A, 28));

        // логотип
        Icon.draw(g, 8, 8 + (int) Math.round(Math.sin(sec * 2) * 2), 3);
        String title = "ClaudeVisuals";
        int tx = 50;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            int col = Hud.mix(0xFFDC7A5A, 0xFF7C5CFF, 0.5 + 0.5 * Math.sin(sec * 2.5 + i * 0.5));
            g.drawString(mc.font, ch, tx, 12, col, true);
            tx += mc.font.width(ch);
        }
        g.drawString(mc.font, "ClaudeCoder · ClaudeCode · smerk3r", 50, 25, 0xFF8A93AD, false);
        g.drawString(mc.font, "1.21.11 · Fabric", 50, 36, 0xFF5A6280, false);

        // справа сверху
        int on = 0;
        for (Mods.Mod m : Mods.ALL) if (m.on) on++;
        String clk = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String info = Mods.ALL.size() + " функций · включено " + on;
        g.drawString(mc.font, clk, w - 6 - mc.font.width(clk), 8, 0xFFFFFFFF, true);
        g.drawString(mc.font, info, w - 6 - mc.font.width(info), 20, 0xFF8A93AD, false);
        String hint = "Right Shift — меню функций";
        g.drawString(mc.font, hint, w - 6 - mc.font.width(hint), 31, Hud.a(0xDC7A5A, (int) (160 + 90 * Math.sin(sec * 3))), false);
    }
}
