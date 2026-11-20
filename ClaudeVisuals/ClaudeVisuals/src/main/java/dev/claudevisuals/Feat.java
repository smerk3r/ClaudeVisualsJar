package dev.claudevisuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;

/** Функции, работающие каждый тик: ночное зрение, авто-спринт, зум, оптимизация, комфорт камеры. */
public final class Feat {
    private static boolean nvApplied, sprintHeld;
    private static Integer savedFov;
    private static Double savedSens;
    private static final Map<String, Object> SAVED = new HashMap<>();

    private Feat() {}

    /** Принудительно ставит значение опции и запоминает старое, чтобы вернуть при выключении. */
    @SuppressWarnings("unchecked")
    private static <T> void force(String key, OptionInstance<T> opt, T value, boolean want) {
        if (want) {
            if (!SAVED.containsKey(key)) SAVED.put(key, opt.get());
            if (!opt.get().equals(value)) opt.set(value);
        } else if (SAVED.containsKey(key)) {
            opt.set((T) SAVED.remove(key));
        }
    }

    public static void tick(Minecraft mc) {
        Music.hook(mc);
        Options o = mc.options;

        Mods.Mod b = Mods.get("boost"), c = Mods.get("comfort");
        force("fps", o.framerateLimit(), 260, b.on && b.b("unl"));
        force("vsync", o.enableVsync(), false, b.on && b.b("vsync"));
        force("part", o.particles(), ParticleStatus.MINIMAL, b.on && b.b("part"));
        force("shadow", o.entityShadows(), false, b.on && b.b("shadow"));
        force("fovfx", o.fovEffectScale(), 0.0, c.on && c.b("fov"));
        force("bob", o.bobView(), false, c.on && c.b("bob"));
        force("dist", o.screenEffectScale(), 0.0, c.on && c.b("dist"));

        if (mc.player == null) { nvApplied = false; sprintHeld = false; return; }

        // ---- ночное зрение (клиентский эффект, сервер о нём не знает) ----
        Mods.Mod nv = Mods.get("nv");
        if (nv.on) {
            MobEffectInstance cur = mc.player.getEffect(MobEffects.NIGHT_VISION);
            if (cur == null || cur.getDuration() < 240) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, false));
                nvApplied = true;
            }
        } else if (nvApplied) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
            nvApplied = false;
        }

        // ---- авто-спринт ----
        Mods.Mod sp = Mods.get("sprint");
        boolean want = sp.on && mc.screen == null && o.keyUp.isDown() && !mc.player.isShiftKeyDown();
        if (want) { o.keySprint.setDown(true); sprintHeld = true; }
        else if (sprintHeld) { o.keySprint.setDown(false); sprintHeld = false; }

        // ---- зум ----
        Mods.Mod z = Mods.get("zoom");
        boolean zooming = z.on && ClaudeVisualsClient.zoomKey.isDown() && mc.screen == null;
        OptionInstance<Integer> fov = o.fov();
        if (zooming) {
            if (savedFov == null) { savedFov = fov.get(); savedSens = o.sensitivity().get(); }
            int target = (int) z.n("fov"), cur = fov.get();
            int next = z.b("smooth") ? cur + Integer.signum(target - cur) * Math.max(1, Math.abs(target - cur) / 4) : target;
            if (Math.abs(target - cur) <= 1) next = target;
            fov.set(next);
            if (z.b("sens")) o.sensitivity().set(savedSens * 0.4);
        } else if (savedFov != null) {
            fov.set(savedFov);
            o.sensitivity().set(savedSens);
            savedFov = null;
        }
    }

    public static boolean zooming() { return savedFov != null; }
    public static boolean sprinting() { return sprintHeld; }
}
