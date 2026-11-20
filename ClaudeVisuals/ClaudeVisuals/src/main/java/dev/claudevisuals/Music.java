package dev.claudevisuals;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

import java.util.Locale;
import java.util.Map;

/** Определяет, какой музыкальный трек / пластинка играет сейчас. */
public final class Music {
    public static String title = "—";
    public static long start;
    private static boolean hooked;

    private static final Map<String, String> NAMES = Map.ofEntries(
        Map.entry("calm1", "C418 – Minecraft"), Map.entry("calm2", "C418 – Clark"), Map.entry("calm3", "C418 – Sweden"),
        Map.entry("hal1", "C418 – Subwoofer Lullaby"), Map.entry("hal2", "C418 – Living Mice"),
        Map.entry("hal3", "C418 – Haggstrom"), Map.entry("hal4", "C418 – Danny"),
        Map.entry("nuance1", "C418 – Key"), Map.entry("nuance2", "C418 – Oxygène"),
        Map.entry("piano1", "C418 – Dry Hands"), Map.entry("piano2", "C418 – Wet Hands"),
        Map.entry("cat", "Диск: C418 – cat"), Map.entry("blocks", "Диск: C418 – blocks"), Map.entry("chirp", "Диск: C418 – chirp"),
        Map.entry("far", "Диск: C418 – far"), Map.entry("mall", "Диск: C418 – mall"), Map.entry("mellohi", "Диск: C418 – mellohi"),
        Map.entry("stal", "Диск: C418 – stal"), Map.entry("strad", "Диск: C418 – strad"), Map.entry("ward", "Диск: C418 – ward"),
        Map.entry("wait", "Диск: C418 – wait"), Map.entry("pigstep", "Диск: Lena Raine – Pigstep"),
        Map.entry("otherside", "Диск: Lena Raine – otherside"), Map.entry("relic", "Диск: Aaron Cherof – Relic"),
        Map.entry("precipice", "Диск: Aaron Cherof – Precipice"), Map.entry("creator", "Диск: Lena Raine – Creator"));

    private Music() {}

    /** Подписываемся на звуковую систему (один раз, когда она уже создана). */
    public static void hook(Minecraft mc) {
        if (hooked || mc.getSoundManager() == null) return;
        hooked = true;
        mc.getSoundManager().addListener((snd, set, range) -> {
            try {
                SoundSource src = snd.getSource();
                if (src != SoundSource.MUSIC && src != SoundSource.RECORDS) return;
                String id = snd.getIdentifier().getPath();
                String file = snd.getSound() != null ? snd.getSound().getLocation().getPath() : id;
                title = pretty(file);
                start = System.currentTimeMillis();
            } catch (Throwable ignored) { }
        });
    }

    private static String pretty(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        String known = NAMES.get(name.toLowerCase(Locale.ROOT));
        if (known != null) return known;
        StringBuilder sb = new StringBuilder();
        for (String part : name.split("[_\\-]")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    public static String elapsed() {
        if (start == 0) return "";
        long s = (System.currentTimeMillis() - start) / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
