package dev.claudevisuals;

import net.minecraft.client.gui.GuiGraphics;

/** Пиксельный значок ClaudeVisuals (12x8), рисуется прямоугольниками, без текстур. */
public final class Icon {
    private static final String[] ROWS = {
        "..########..",
        "..#.####.#..",
        "############",
        "############",
        "..########..",
        "..########..",
        "..#.#..#.#..",
        "..#.#..#.#.."
    };

    private Icon() {}

    public static void draw(GuiGraphics g, int x, int y, int s) { draw(g, x, y, s, 0xFFDC7A5A); }

    public static void draw(GuiGraphics g, int x, int y, int s, int color) {
        for (int r = 0; r < ROWS.length; r++) {
            String row = ROWS[r];
            int c = 0;
            while (c < row.length()) {
                if (row.charAt(c) == '#') {
                    int e = c;
                    while (e < row.length() && row.charAt(e) == '#') e++;
                    g.fill(x + c * s, y + r * s, x + e * s, y + (r + 1) * s, color);
                    c = e;
                } else c++;
            }
        }
    }
}
