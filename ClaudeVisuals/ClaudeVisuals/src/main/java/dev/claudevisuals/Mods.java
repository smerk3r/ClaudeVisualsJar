package dev.claudevisuals;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Реестр всех функций (модулей) + сохранение настроек в config/claudevisuals.json */
public final class Mods {
    public static final String[] PN = {"Claude", "Фиолет", "Циан", "Роза", "Лайм", "Янтарь", "Белый", "Красный", "Чёрный"};
    public static final int[] PC = {0xFFDC7A5A, 0xFF7C5CFF, 0xFF2DE2E6, 0xFFFF5FA2, 0xFF8BE04A, 0xFFFFB02E, 0xFFFFFFFF, 0xFFFF5555, 0xFF000000};

    public static final class Setting {
        public final String id, name;
        public final int type; // 0 bool, 1 num, 2 mode, 3 color
        public boolean b;
        public double n, min, max, step;
        public int idx;
        public String[] modes;

        Setting(String id, String name, int type) { this.id = id; this.name = name; this.type = type; }

        public static Setting bool(String id, String name, boolean d) { Setting s = new Setting(id, name, 0); s.b = d; return s; }
        public static Setting num(String id, String name, double d, double min, double max, double step) {
            Setting s = new Setting(id, name, 1); s.n = d; s.min = min; s.max = max; s.step = step; return s;
        }
        public static Setting mode(String id, String name, int d, String... modes) {
            Setting s = new Setting(id, name, 2); s.idx = d; s.modes = modes; return s;
        }
        public static Setting color(String id, String name, int d) { Setting s = new Setting(id, name, 3); s.idx = d; return s; }

        public String fmt() {
            switch (type) {
                case 0: return b ? "ВКЛ" : "ВЫКЛ";
                case 1: return step >= 1 ? String.valueOf((int) Math.round(n)) : String.format(Locale.ROOT, "%.2f", n);
                case 2: return modes[idx];
                default: return PN[idx];
            }
        }

        public void cycle() {
            if (type == 0) b = !b;
            else if (type == 2) idx = (idx + 1) % modes.length;
            else if (type == 3) idx = (idx + 1) % PC.length;
        }
    }

    public static final class Mod {
        public final String id, cat, name, desc;
        public boolean on;
        public final boolean hud;
        public final List<Setting> s = new ArrayList<>();
        public net.minecraft.client.KeyMapping key;   // бинд включения/выключения (создаётся при запуске)
        public int defKey = -1;                         // клавиша по умолчанию (-1 = не назначена)

        Mod(String id, String cat, String name, String desc, boolean on, boolean hud, Setting... st) {
            this.id = id; this.cat = cat; this.name = name; this.desc = desc; this.on = on; this.hud = hud;
            s.addAll(Arrays.asList(st));
        }
        public Setting g(String k) {
            for (Setting x : s) if (x.id.equals(k)) return x;
            throw new IllegalArgumentException(k);
        }
        public boolean b(String k) { return g(k).b; }
        public double n(String k) { return g(k).n; }
        public int i(String k) { return (int) Math.round(g(k).n); }
        public int m(String k) { return g(k).idx; }
        public int c(String k) { return PC[g(k).idx]; }
    }

    public static final List<Mod> ALL = new ArrayList<>();
    private static final Map<String, Mod> BY = new HashMap<>();

    public static Mod get(String id) { return BY.get(id); }

    private static Mod add(Mod m) { ALL.add(m); BY.put(m.id, m); return m; }

    private static Mod hud(String id, String name, String desc, boolean on, int x, int y, Setting... extra) {
        List<Setting> l = new ArrayList<>(List.of(
            Setting.num("x", "Позиция X, %", x, 0, 100, 1),
            Setting.num("y", "Позиция Y, %", y, 0, 100, 1),
            Setting.color("col", "Цвет", 0),
            Setting.bool("bg", "Фон", true)));
        l.addAll(Arrays.asList(extra));
        return add(new Mod(id, "HUD", name, desc, on, true, l.toArray(new Setting[0])));
    }

    private static Mod mod(String id, String cat, String name, String desc, boolean on, Setting... st) {
        return add(new Mod(id, cat, name, desc, on, false, st));
    }

    static {
        // ---------- HUD ----------
        hud("fps", "FPS", "Частота кадров", true, 1, 2);
        hud("cps", "CPS", "Клики в секунду", true, 1, 8, Setting.bool("rmb", "Показывать ПКМ", true));
        hud("ping", "Пинг", "Задержка до сервера", true, 1, 14);
        hud("coords", "Координаты", "XYZ и направление", true, 1, 20, Setting.bool("dec", "Десятичные", false), Setting.bool("dir", "Направление", true));
        hud("speed", "Скорость", "Блоков в секунду", false, 1, 26);
        hud("biome", "Биом", "Текущий биом", false, 1, 32);
        hud("clock", "Часы", "Реальное время и дни мира", true, 99, 2, Setting.bool("secs", "Секунды", false), Setting.bool("game", "Дни мира", true));
        hud("keys", "Клавиши", "WASD, пробел и мышь: куда нажимать", true, 1, 45, Setting.bool("mouse", "Мышь + CPS", true), Setting.bool("space", "Пробел", true));
        hud("armor", "Броня", "Броня и предмет в руке", true, 99, 50, Setting.bool("dur", "Прочность", true));
        hud("potion", "Эффекты", "Активные эффекты зелий", true, 99, 12);
        hud("music", "Музыка", "Какой трек играет сейчас", true, 1, 96);
        hud("hints", "Подсказки клавиш", "Список клавиш мода", true, 1, 62);
        hud("server", "Сервер", "Адрес, пинг и онлайн", false, 99, 28);
        hud("mem", "Память", "Использование RAM", false, 99, 34);
        hud("combo", "Комбо", "Серия ударов", true, 50, 60);
        hud("items", "Счётчик предмета", "Сколько таких предметов в инвентаре", false, 99, 40);
        hud("status", "Статус", "Авто-спринт, ночное зрение, зум", true, 1, 52);

        // ---------- Визуал ----------
        mod("crosshair", "Визуал", "Прицел", "Свой прицел вместо ванильного", true,
            Setting.mode("st", "Стиль", 0, "Крест", "Точка", "Круг", "Квадрат"),
            Setting.num("size", "Размер", 5, 1, 16, 1), Setting.num("gap", "Зазор", 2, 0, 10, 1),
            Setting.num("th", "Толщина", 1, 1, 4, 1), Setting.color("col", "Цвет", 6),
            Setting.bool("ol", "Обводка", true), Setting.bool("dyn", "Динамический", true));
        mod("hitmarker", "Визуал", "Хитмаркер", "Крестик при попадании по сущности", true,
            Setting.color("col", "Цвет", 7), Setting.num("size", "Размер", 6, 3, 12, 1), Setting.num("ms", "Длительность, мс", 250, 100, 800, 50));
        mod("vignette", "Визуал", "Виньетка", "Затемнение по краям экрана", false,
            Setting.num("str", "Сила", 40, 5, 90, 5), Setting.color("col", "Цвет", 8));
        mod("border", "Визуал", "Неоновая рамка", "Анимированная рамка экрана", false,
            Setting.color("col", "Цвет", 0), Setting.num("w", "Толщина", 2, 1, 6, 1), Setting.num("spd", "Скорость", 4, 1, 10, 1));
        mod("particles", "Визуал", "Частицы экрана", "Снег, искры, пузырьки поверх картинки", false,
            Setting.mode("t", "Тип", 0, "Снег", "Искры", "Пузыри"), Setting.num("n", "Количество", 50, 10, 150, 5), Setting.color("col", "Цвет", 6));
        mod("nv", "Визуал", "Ночное зрение", "Хорошо видно в темноте (клавиша N)", false);
        mod("zoom", "Визуал", "Зум", "Приближение, удерживайте C", true,
            Setting.num("fov", "FOV при зуме", 30, 30, 70, 1), Setting.bool("smooth", "Плавно", true), Setting.bool("sens", "Меньше чувствительность", true));
        mod("comfort", "Визуал", "Комфорт камеры", "Убрать покачивание и искажения", false,
            Setting.bool("fov", "Без смены FOV", true), Setting.bool("bob", "Без покачивания", true), Setting.bool("dist", "Без искажений", false));

        // ---------- Движение ----------
        mod("sprint", "Движение", "Авто-спринт", "Бежать вперёд без удержания Ctrl (клавиша V)", false);

        // ---------- Оптимизация ----------
        mod("boost", "Оптимизация", "FPS-буст", "Снять лимит FPS, меньше частиц и теней", false,
            Setting.bool("unl", "Без лимита FPS", true), Setting.bool("vsync", "Выключить VSync", true),
            Setting.bool("part", "Минимум частиц", true), Setting.bool("shadow", "Без теней сущностей", true));
        mod("hudlite", "Оптимизация", "Лёгкий HUD", "Без фонов и теней у модулей", false);

        // ---------- Таб ----------
        mod("tab", "Таб", "Анимированный таб", "Красивый список игроков (клавиша Tab)", true,
            Setting.bool("badge", "Значок у вас", true), Setting.bool("ping", "Показывать пинг", true),
            Setting.num("rows", "Строк в колонке", 12, 6, 24, 1), Setting.color("col", "Цвет", 0),
            Setting.num("alpha", "Прозрачность", 200, 80, 255, 5));
    }

    static {
        get("nv").defKey = 78;      // N
        get("sprint").defKey = 86;  // V
    }

    // ---------- конфиг ----------
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("claudevisuals.json"); }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Mod m : ALL) {
                JsonObject o = new JsonObject();
                o.addProperty("on", m.on);
                JsonObject st = new JsonObject();
                for (Setting s : m.s) {
                    switch (s.type) {
                        case 0 -> st.addProperty(s.id, s.b);
                        case 1 -> st.addProperty(s.id, s.n);
                        default -> st.addProperty(s.id, s.idx);
                    }
                }
                o.add("s", st);
                root.add(m.id, o);
            }
            Files.writeString(file(), new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ignored) { }
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) { save(); return; }
            JsonObject root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            for (Mod m : ALL) {
                if (!root.has(m.id)) continue;
                JsonObject o = root.getAsJsonObject(m.id);
                if (o.has("on")) m.on = o.get("on").getAsBoolean();
                if (!o.has("s")) continue;
                JsonObject st = o.getAsJsonObject("s");
                for (Setting s : m.s) {
                    if (!st.has(s.id)) continue;
                    JsonElement e = st.get(s.id);
                    switch (s.type) {
                        case 0 -> s.b = e.getAsBoolean();
                        case 1 -> s.n = Math.max(s.min, Math.min(s.max, e.getAsDouble()));
                        case 2 -> s.idx = Math.floorMod(e.getAsInt(), s.modes.length);
                        default -> s.idx = Math.floorMod(e.getAsInt(), PC.length);
                    }
                }
            }
        } catch (Exception ignored) { }
    }
}
