package dev.claudevisuals;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Весь HUD: модули, оверлеи, прицел, хитмаркер и анимированный таб. */
public final class Hud {
    private static long fpsT = System.nanoTime();
    private static int frames, fps;
    private static final ArrayDeque<Long> LC = new ArrayDeque<>(), RC = new ArrayDeque<>();
    private static boolean pl, pr;
    private static int combo;
    private static long lastHit, hitMark;
    private static double speed;
    private static float tabAnim;

    private static final float[] RX = new float[160], RY = new float[160], RS = new float[160];
    static {
        Random r = new Random(42);
        for (int i = 0; i < 160; i++) { RX[i] = r.nextFloat(); RY[i] = r.nextFloat(); RS[i] = 0.3f + r.nextFloat(); }
    }

    private Hud() {}

    // ---------- утилиты ----------
    public static int a(int rgb, int alpha) { return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF); }

    public static int mix(int c1, int c2, double k) {
        int r = (int) (((c1 >> 16) & 255) * (1 - k) + ((c2 >> 16) & 255) * k);
        int g = (int) (((c1 >> 8) & 255) * (1 - k) + ((c2 >> 8) & 255) * k);
        int b = (int) ((c1 & 255) * (1 - k) + (c2 & 255) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ---------- главный HUD ----------
    public static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || ClaudeVisualsClient.hudHidden) return;
        frame(mc);
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        boolean lite = Mods.get("hudlite").on;
        long now = System.currentTimeMillis();
        try { overlays(g, sw, sh, now, lite); } catch (RuntimeException ignored) { }
        for (Mods.Mod m : Mods.ALL) {
            if (!m.hud || !m.on) continue;
            try { module(g, mc, m, sw, sh, now, lite); } catch (RuntimeException ignored) { }
        }
        hitmarker(g, sw, sh, now);
    }

    private static void frame(Minecraft mc) {
        long n = System.nanoTime();
        frames++;
        if (n - fpsT >= 500_000_000L) { fps = (int) (frames * 1e9 / (n - fpsT)); frames = 0; fpsT = n; }
        long ms = System.currentTimeMillis();
        boolean ca = mc.options.keyAttack.isDown() && mc.screen == null;
        boolean cu = mc.options.keyUse.isDown() && mc.screen == null;
        if (ca && !pl) {
            LC.add(ms);
            if (mc.hitResult instanceof EntityHitResult) { combo++; lastHit = ms; hitMark = ms; }
        }
        if (cu && !pr) RC.add(ms);
        pl = ca; pr = cu;
        while (!LC.isEmpty() && ms - LC.peek() > 1000) LC.poll();
        while (!RC.isEmpty() && ms - RC.peek() > 1000) RC.poll();
        if (combo > 0 && ms - lastHit > 2000) combo = 0;
        LocalPlayer p = mc.player;
        speed = Math.hypot(p.getX() - p.xo, p.getZ() - p.zo) * 20;
    }

    // ---------- модули ----------
    private static void module(GuiGraphics g, Minecraft mc, Mods.Mod m, int sw, int sh, long now, boolean lite) {
        LocalPlayer p = mc.player;
        switch (m.id) {
            case "fps" -> line(g, mc, m, sw, sh, "FPS", String.valueOf(fps), lite);
            case "cps" -> line(g, mc, m, sw, sh, "CPS", LC.size() + (m.b("rmb") ? " | " + RC.size() : ""), lite);
            case "ping" -> line(g, mc, m, sw, sh, "Пинг", ping(mc) + " мс", lite);
            case "coords" -> {
                String xyz = m.b("dec")
                    ? String.format(Locale.ROOT, "%.1f %.1f %.1f", p.getX(), p.getY(), p.getZ())
                    : (int) Math.floor(p.getX()) + " " + (int) Math.floor(p.getY()) + " " + (int) Math.floor(p.getZ());
                String[] d = {"Ю", "ЮЗ", "З", "СЗ", "С", "СВ", "В", "ЮВ"};
                String dir = m.b("dir") ? " " + d[Math.floorMod(Math.round(Mth.wrapDegrees(p.getYRot()) / 45f), 8)] : "";
                line(g, mc, m, sw, sh, "XYZ", xyz + dir, lite);
            }
            case "speed" -> line(g, mc, m, sw, sh, "Скорость", String.format(Locale.ROOT, "%.2f б/с", speed), lite);
            case "biome" -> {
                String id = mc.level.getBiome(p.blockPosition()).getRegisteredName().replace("minecraft:", "");
                line(g, mc, m, sw, sh, "Биом", id, lite);
            }
            case "clock" -> {
                String t = LocalTime.now().format(DateTimeFormatter.ofPattern(m.b("secs") ? "HH:mm:ss" : "HH:mm"));
                if (m.b("game")) t += "  день " + (mc.level.getGameTime() / 24000L + 1);
                line(g, mc, m, sw, sh, "Время", t, lite);
            }
            case "keys" -> keys(g, mc, m, sw, sh, lite);
            case "armor" -> armor(g, mc, m, sw, sh, lite);
            case "potion" -> potion(g, mc, m, sw, sh, lite);
            case "music" -> {
                String t = Music.title.equals("—") ? "—" : Music.title + "  " + Music.elapsed();
                line(g, mc, m, sw, sh, "♪", t, lite);
            }
            case "hints" -> hints(g, mc, m, sw, sh, lite);
            case "server" -> {
                ServerData sd = mc.getCurrentServer();
                ClientPacketListener c = mc.getConnection();
                List<String[]> l = new ArrayList<>();
                l.add(new String[]{"Сервер", sd != null ? sd.ip : "Одиночная игра"});
                l.add(new String[]{"Пинг", ping(mc) + " мс"});
                if (c != null) l.add(new String[]{"Онлайн", String.valueOf(c.getOnlinePlayers().size())});
                lines(g, mc, m, sw, sh, l, lite);
            }
            case "mem" -> {
                Runtime rt = Runtime.getRuntime();
                long used = (rt.totalMemory() - rt.freeMemory()) >> 20, max = rt.maxMemory() >> 20;
                line(g, mc, m, sw, sh, "RAM", used + "/" + max + " МБ", lite);
            }
            case "combo" -> { if (combo > 1) line(g, mc, m, sw, sh, "Комбо", "x" + combo, lite); }
            case "items" -> {
                ItemStack h = p.getMainHandItem();
                if (h.isEmpty()) return;
                Inventory inv = p.getInventory();
                int c = 0;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    if (ItemStack.isSameItem(s, h)) c += s.getCount();
                }
                line(g, mc, m, sw, sh, h.getHoverName().getString(), "x" + c, lite);
            }
            case "status" -> {
                List<String> st = new ArrayList<>();
                if (Feat.sprinting()) st.add("Спринт");
                if (Mods.get("nv").on) st.add("Ночное зрение");
                if (Feat.zooming()) st.add("Зум");
                if (!st.isEmpty()) line(g, mc, m, sw, sh, "CV", String.join(" · ", st), lite);
            }
            default -> { }
        }
    }

    private static int ping(Minecraft mc) {
        ClientPacketListener c = mc.getConnection();
        if (c == null || mc.player == null) return 0;
        PlayerInfo i = c.getPlayerInfo(mc.player.getUUID());
        return i == null ? 0 : i.getLatency();
    }

    private static void box(GuiGraphics g, Mods.Mod m, int x, int y, int w, int h, boolean lite) {
        if (lite || !m.b("bg")) return;
        g.fill(x, y, x + w, y + h, 0x8C0A0C14);
        g.fill(x, y, x + 1, y + h, m.c("col"));
    }

    private static void line(GuiGraphics g, Minecraft mc, Mods.Mod m, int sw, int sh, String label, String val, boolean lite) {
        lines(g, mc, m, sw, sh, List.<String[]>of(new String[]{label, val}), lite);
    }

    private static void lines(GuiGraphics g, Minecraft mc, Mods.Mod m, int sw, int sh, List<String[]> rows, boolean lite) {
        Font f = mc.font;
        int w = 0;
        for (String[] r : rows) w = Math.max(w, f.width(r[0] + ": " + r[1]));
        w += 8;
        int h = rows.size() * 10 + 3;
        int x = (int) ((sw - w) * m.n("x") / 100.0), y = (int) ((sh - h) * m.n("y") / 100.0);
        box(g, m, x, y, w, h, lite);
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            g.drawString(f, r[0] + ":", x + 4, y + 2 + i * 10, m.c("col"), !lite);
            g.drawString(f, r[1], x + 4 + f.width(r[0] + ": "), y + 2 + i * 10, 0xFFFFFFFF, !lite);
        }
    }

    private static void keyBox(GuiGraphics g, Font f, int x, int y, int w, int h, String t, boolean down, int col, boolean lite) {
        if (!lite || down) g.fill(x, y, x + w, y + h, down ? Hud.a(col, 0xCC) : 0x8C0A0C14);
        if (!t.isEmpty()) g.drawString(f, t, x + (w - f.width(t)) / 2, y + (h - 8) / 2 + 1, 0xFFFFFFFF, false);
    }

    private static void keys(GuiGraphics g, Minecraft mc, Mods.Mod m, int sw, int sh, boolean lite) {
        Font f = mc.font;
        Options o = mc.options;
        boolean mouse = m.b("mouse"), space = m.b("space");
        int w = 64, h = 42 + (mouse ? 18 : 0) + (space ? 10 : 0);
        int x = (int) ((sw - w) * m.n("x") / 100.0), y = (int) ((sh - h) * m.n("y") / 100.0), col = m.c("col");
        keyBox(g, f, x + 22, y, 20, 20, "W", o.keyUp.isDown(), col, lite);
        keyBox(g, f, x, y + 22, 20, 20, "A", o.keyLeft.isDown(), col, lite);
        keyBox(g, f, x + 22, y + 22, 20, 20, "S", o.keyDown.isDown(), col, lite);
        keyBox(g, f, x + 44, y + 22, 20, 20, "D", o.keyRight.isDown(), col, lite);
        int yy = y + 44;
        if (mouse) {
            keyBox(g, f, x, yy, 31, 16, "L " + LC.size(), pl, col, lite);
            keyBox(g, f, x + 33, yy, 31, 16, "R " + RC.size(), pr, col, lite);
            yy += 18;
        }
        if (space) keyBox(g, f, x, yy, 64, 8, "", o.keyJump.isDown(), col, lite);
    }

    private static void armor(GuiGraphics g, Minecraft mc, Mods.Mod m, int sw, int sh, boolean lite) {
        LocalPlayer p = mc.player;
        Font f = mc.font;
        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = p.getItemBySlot(s);
            if (!st.isEmpty()) items.add(st);
        }
        if (!p.getMainHandItem().isEmpty()) items.add(p.getMainHandItem());
        if (items.isEmpty()) return;
        boolean dur = m.b("dur");
        int w = 20 + (dur ? 30 : 0), h = items.size() * 18 + 2;
        int x = (int) ((sw - w) * m.n("x") / 100.0), y = (int) ((sh - h) * m.n("y") / 100.0);
        box(g, m, x, y, w, h, lite);
        for (int i = 0; i < items.size(); i++) {
            ItemStack st = items.get(i);
            int iy = y + 1 + i * 18;
            g.renderItem(st, x + 2, iy);
            g.renderItemDecorations(f, st, x + 2, iy);
            if (dur && st.isDamageableItem()) {
                int left = st.getMaxDamage() - st.getDamageValue();
                double k = left / (double) st.getMaxDamage();
                g.drawString(f, String.valueOf(left), x + 20, iy + 4, k > 0.5 ? 0xFF8BE04A : k > 0.2 ? 0xFFFFB02E : 0xFFFF5555, !lite);
            }
        }
    }

    private static void potion(GuiGraphics g, Minecraft mc, Mods.Mod m, int sw, int sh, boolean lite) {
        Font f = mc.font;
        List<String> txt = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        for (MobEffectInstance e : mc.player.getActiveEffects()) {
            int s = e.getDuration() / 20;
            String time = e.isInfiniteDuration() ? "∞" : String.format("%d:%02d", s / 60, s % 60);
            txt.add(e.getEffect().value().getDisplayName().getString() + " " + (e.getAmplifier() + 1) + "  " + time);
            cols.add(e.getEffect().value().isBeneficial() ? 0xFF8BE04A : 0xFFFF5555);
        }
        if (txt.isEmpty()) return;
        int w = 0;
        for (String t : txt) w = Math.max(w, f.width(t));
        w += 8;
        int h = txt.size() * 10 + 3;
        int x = (int) ((sw - w) * m.n("x") / 100.0), y = (int) ((sh - h) * m.n("y") / 100.0);
        box(g, m, x, y, w, h, lite);
        for (int i = 0; i < txt.size(); i++) g.drawString(f, txt.get(i), x + 4, y + 2 + i * 10, cols.get(i), !lite);
    }

    private static String kn(net.minecraft.client.KeyMapping k) { return k.getTranslatedKeyMessage().getString(); }

    private static void hints(GuiGraphics g, Minecraft mc, Mods.Mod m, int sw, int sh, boolean lite) {
        List<String[]> l = new ArrayList<>();
        if (!ClaudeVisualsClient.menuKey.isUnbound()) l.add(new String[]{kn(ClaudeVisualsClient.menuKey), "меню функций"});
        if (!ClaudeVisualsClient.zoomKey.isUnbound()) l.add(new String[]{kn(ClaudeVisualsClient.zoomKey), "зум (держать)"});
        if (!ClaudeVisualsClient.hudKey.isUnbound()) l.add(new String[]{kn(ClaudeVisualsClient.hudKey), "скрыть HUD"});
        if (!mc.options.keyPlayerList.isUnbound()) l.add(new String[]{kn(mc.options.keyPlayerList), "таб-лист"});
        for (Mods.Mod o : Mods.ALL) if (o.key != null && !o.key.isUnbound()) l.add(new String[]{kn(o.key), o.name});
        lines(g, mc, m, sw, sh, l, lite);
    }

    // ---------- оверлеи ----------
    private static void overlays(GuiGraphics g, int sw, int sh, long now, boolean lite) {
        Mods.Mod v = Mods.get("vignette");
        if (v.on && !lite) {
            int k = Math.max(2, sh / 60), col = v.c("col") & 0xFFFFFF;
            double str = v.n("str") * 2.55;
            for (int i = 0; i < 14; i++) {
                int al = (int) (str * Math.pow(1 - i / 14.0, 2));
                g.fill(0, i * k, sw, (i + 1) * k, a(col, al));
                g.fill(0, sh - (i + 1) * k, sw, sh - i * k, a(col, al));
                g.fill(i * k, 0, (i + 1) * k, sh, a(col, al / 2));
                g.fill(sw - (i + 1) * k, 0, sw - i * k, sh, a(col, al / 2));
            }
        }
        Mods.Mod b = Mods.get("border");
        if (b.on) {
            int w = b.i("w"), col = b.c("col") & 0xFFFFFF;
            double ph = now * b.n("spd") / 700.0;
            for (int x = 0; x < sw; x += 8) {
                int al = (int) (120 + 110 * Math.sin(ph + x * 0.02));
                g.fill(x, 0, Math.min(sw, x + 8), w, a(col, al));
                g.fill(x, sh - w, Math.min(sw, x + 8), sh, a(col, al));
            }
            for (int y = 0; y < sh; y += 8) {
                int al = (int) (120 + 110 * Math.sin(ph + y * 0.02 + 2));
                g.fill(0, y, w, Math.min(sh, y + 8), a(col, al));
                g.fill(sw - w, y, sw, Math.min(sh, y + 8), a(col, al));
            }
        }
        Mods.Mod p = Mods.get("particles");
        if (p.on) {
            int n = p.i("n"), type = p.m("t"), col = p.c("col") & 0xFFFFFF;
            float t = now / 1000f;
            for (int i = 0; i < n; i++) {
                float s = RS[i];
                float y = type == 0 ? ((RY[i] + t * 0.04f * s) % 1f) : (1f - ((RY[i] + t * 0.05f * s) % 1f));
                float x = (((RX[i] + (float) Math.sin(t * 0.6f + i) * 0.015f) % 1f) + 1f) % 1f;
                int px = (int) (x * sw), py = (int) (y * sh);
                int al = type == 1 ? (int) (200 * (1 - y)) : 170;
                int sz = type == 2 ? 3 : (s > 0.9f ? 2 : 1);
                g.fill(px, py, px + sz, py + sz, a(type == 1 ? 0xFFB02E : col, al));
            }
        }
    }

    private static void hitmarker(GuiGraphics g, int sw, int sh, long now) {
        Mods.Mod m = Mods.get("hitmarker");
        if (!m.on) return;
        long age = now - hitMark;
        if (hitMark == 0 || age > m.i("ms")) return;
        int al = (int) (255 * (1 - age / (double) m.i("ms"))), col = m.c("col") & 0xFFFFFF, cx = sw / 2, cy = sh / 2;
        int size = m.i("size");
        for (int i = 3; i < 3 + size; i++) {
            g.fill(cx - i, cy - i, cx - i + 1, cy - i + 1, a(col, al));
            g.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, a(col, al));
            g.fill(cx - i, cy + i, cx - i + 1, cy + i + 1, a(col, al));
            g.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, a(col, al));
        }
    }

    // ---------- прицел ----------
    public static void crosshairElement(HudElement orig, GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        Mods.Mod m = Mods.get("crosshair");
        if (!m.on || mc.player == null || !mc.options.getCameraType().isFirstPerson()) { orig.render(g, dt); return; }
        int cx = mc.getWindow().getGuiScaledWidth() / 2, cy = mc.getWindow().getGuiScaledHeight() / 2;
        int col = m.c("col"), size = m.i("size"), th = m.i("th"), half = th / 2;
        int gp = m.i("gap");
        if (m.b("dyn")) gp += (mc.player.isSprinting() ? 3 : 0) + (System.currentTimeMillis() - hitMark < 150 ? 2 : 0);
        boolean ol = m.b("ol");
        switch (m.m("st")) {
            case 0 -> {
                r(g, cx - gp - size, cy - half, cx - gp, cy - half + th, col, ol);
                r(g, cx + gp, cy - half, cx + gp + size, cy - half + th, col, ol);
                r(g, cx - half, cy - gp - size, cx - half + th, cy - gp, col, ol);
                r(g, cx - half, cy + gp, cx - half + th, cy + gp + size, col, ol);
            }
            case 1 -> { int d = th + 1; r(g, cx - d / 2, cy - d / 2, cx - d / 2 + d, cy - d / 2 + d, col, ol); }
            case 2 -> {
                double R = size + gp;
                for (int i = 0; i < 48; i++) {
                    int px = cx + (int) Math.round(R * Math.cos(i * Math.PI / 24)), py = cy + (int) Math.round(R * Math.sin(i * Math.PI / 24));
                    g.fill(px, py, px + th, py + th, col);
                }
            }
            default -> {
                int R = size / 2 + gp;
                r(g, cx - R, cy - R, cx + R, cy - R + th, col, ol);
                r(g, cx - R, cy + R - th, cx + R, cy + R, col, ol);
                r(g, cx - R, cy - R, cx - R + th, cy + R, col, ol);
                r(g, cx + R - th, cy - R, cx + R, cy + R, col, ol);
            }
        }
    }

    private static void r(GuiGraphics g, int x1, int y1, int x2, int y2, int col, boolean ol) {
        if (ol) g.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xFF000000);
        g.fill(x1, y1, x2, y2, col);
    }

    // ---------- анимированный таб ----------
    public static void tabElement(HudElement orig, GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        Mods.Mod m = Mods.get("tab");
        if (!m.on) { orig.render(g, dt); return; }
        float target = mc.options.keyPlayerList.isDown() ? 1f : 0f;
        tabAnim += (target - tabAnim) * 0.22f;
        if (Math.abs(target - tabAnim) < 0.01f) tabAnim = target;
        ClientPacketListener c = mc.getConnection();
        if (tabAnim <= 0.02f || c == null || mc.player == null) return;

        Font f = mc.font;
        List<PlayerInfo> list = new ArrayList<>(c.getOnlinePlayers());
        list.sort(Comparator.comparing((PlayerInfo i) -> i.getProfile().name().toLowerCase(Locale.ROOT)));
        int n = list.size(), rows = m.i("rows"), cols = Math.max(1, (n + rows - 1) / rows);
        int colW = 156, rowH = 11;
        int w = Math.min(cols * colW + 12, mc.getWindow().getGuiScaledWidth() - 4);
        int h = Math.min(n, rows) * rowH + 42;
        int sw = mc.getWindow().getGuiScaledWidth();
        int x = (sw - w) / 2, y = (int) (8 - (1 - tabAnim) * 34);
        int accent = m.c("col"), base = (int) (m.i("alpha") * tabAnim);
        long now = System.currentTimeMillis();

        for (int i = 1; i <= 4; i++) g.fill(x - i, y - i, x + w + i, y + h + i, a(accent, (int) (14 * tabAnim)));
        g.fill(x, y, x + w, y + h, a(0x0A0C14, base));
        for (int sx = 0; sx < w; sx += 4) {
            double k = 0.5 + 0.5 * Math.sin(now / 350.0 + sx * 0.04);
            g.fill(x + sx, y, Math.min(x + w, x + sx + 4), y + 2, a(mix(accent, 0xFF7C5CFF, k), (int) (255 * tabAnim)) );
        }
        if (tabAnim < 0.15f) return;

        // шапка
        Icon.draw(g, x + 6, y + 6 + (int) Math.round(Math.sin(now / 300.0)), 1, a(accent, (int) (255 * tabAnim)));
        ServerData sd = mc.getCurrentServer();
        String srv = sd != null ? sd.ip : "Одиночная игра";
        g.drawString(f, "ClaudeVisuals", x + 22, y + 6, a(0xFFFFFF, (int) (255 * tabAnim)), true);
        String right = srv + "  ·  онлайн " + n;
        g.drawString(f, right, x + w - 6 - f.width(right), y + 6, a(0xAAB2CC, (int) (255 * tabAnim)), false);
        g.fill(x + 6, y + 18, x + w - 6, y + 19, a(0xFFFFFF, (int) (30 * tabAnim)));

        // игроки
        for (int idx = 0; idx < n; idx++) {
            int cc = idx / rows, rr = idx % rows;
            float rv = Mth.clamp(tabAnim * 1.7f - idx * 0.015f, 0f, 1f);
            if (rv <= 0.02f) continue;
            int rx = x + 6 + cc * colW + (int) ((1 - rv) * 12), ry = y + 22 + rr * rowH;
            if (rx + colW - 6 > x + w) continue;
            int al = (int) (255 * rv);
            PlayerInfo info = list.get(idx);
            boolean me = info.getProfile().id().equals(mc.player.getUUID());
            g.fill(rx, ry, rx + colW - 6, ry + rowH - 1, a(me ? accent : 0xFFFFFF, me ? (int) (40 * rv) : (idx % 2 == 0 ? (int) (10 * rv) : 0)));
            if (me && m.b("badge")) Icon.draw(g, rx + 2, ry + 1, 1, a(accent, al));
            int nameCol = me ? mix(accent, 0xFFFFFFFF, 0.5 + 0.5 * Math.sin(now / 400.0)) & 0xFFFFFF : 0xFFFFFF;
            g.drawString(f, info.getProfile().name(), rx + 16, ry + 1, a(nameCol, al), true);
            int lat = info.getLatency();
            int bars = lat <= 0 ? 0 : lat < 60 ? 4 : lat < 110 ? 3 : lat < 180 ? 2 : 1;
            int pc = lat < 60 ? 0x55FF55 : lat < 140 ? 0xFFD24D : 0xFF5555;
            for (int b = 0; b < 4; b++) {
                int bh = 2 + b * 2;
                g.fill(rx + colW - 24 + b * 4, ry + 9 - bh, rx + colW - 22 + b * 4, ry + 9, a(b < bars ? pc : 0x444444, al));
            }
            if (m.b("ping")) {
                String ms = lat + "";
                g.drawString(f, ms, rx + colW - 28 - f.width(ms), ry + 1, a(0xAAB2CC, al), false);
            }
        }
        String foot = "ClaudeVisuals · ClaudeCoder · ClaudeCode · smerk3r";
        g.drawString(f, foot, x + (w - f.width(foot)) / 2, y + h - 11, a(0x7A829E, (int) (255 * tabAnim)), false);
    }
}
