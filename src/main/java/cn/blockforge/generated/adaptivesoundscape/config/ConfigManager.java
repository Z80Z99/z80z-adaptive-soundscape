package cn.blockforge.generated.adaptivesoundscape.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ConfigManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Path ROOT = FMLPaths.CONFIGDIR.get().resolve("z80z_adaptive_soundscape");
    public static final Path CONFIG_FILE = ROOT.resolve("rules.json");
    public static final Path CUSTOM_MUSIC = ROOT.resolve("custom_music");
    private static MusicConfig config;
    private static String lastError = "";

    private ConfigManager() {
    }

    /** 旧 modId（adaptive_soundscape）目录一次性迁移到新目录。 */
    public static void migrateLegacyDirectory() {
        Path legacy = FMLPaths.CONFIGDIR.get().resolve("adaptive_soundscape");
        if (!Files.exists(legacy) || Files.exists(ROOT)) return;
        try {
            Files.move(legacy, ROOT, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    public static synchronized MusicConfig get() {
        if (config == null) load();
        return config;
    }

    public static synchronized void load() {
        try {
            Files.createDirectories(ROOT);
            Files.createDirectories(CUSTOM_MUSIC);
            if (!Files.exists(CONFIG_FILE)) {
                config = createPreset("balanced");
                save();
            } else {
                try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                    config = GSON.fromJson(reader, MusicConfig.class);
                }
                validate(config);
            }
            lastError = "";
        } catch (Exception exception) {
            lastError = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            config = createPreset("balanced");
        }
    }

    public static synchronized void save() {
        validate(config);
        try {
            Files.createDirectories(ROOT);
            Path temporary = CONFIG_FILE.resolveSibling("rules.json.tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            Files.move(temporary, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING);
            lastError = "";
        } catch (IOException exception) {
            lastError = exception.getMessage();
        }
    }

    public static synchronized String exportJson() {
        return GSON.toJson(get());
    }

    public static synchronized String importJson(String json) {
        try {
            MusicConfig parsed = GSON.fromJson(json, MusicConfig.class);
            validate(parsed);
            config = parsed;
            save();
            lastImportFailed = false;
            return net.minecraft.client.resources.language.I18n
                    .get("message.z80z_adaptive_soundscape.json.saved");
        } catch (JsonParseException | IllegalArgumentException exception) {
            lastImportFailed = true;
            return net.minecraft.client.resources.language.I18n
                    .get("message.z80z_adaptive_soundscape.json.invalid", exception.getMessage());
        }
    }

    public static boolean lastImportFailed() {
        return lastImportFailed;
    }

    private static boolean lastImportFailed;

    public static synchronized void applyPreset(String name) {
        config = createPreset(name);
        save();
    }

    public static String lastError() {
        return lastError;
    }

    private static void validate(MusicConfig value) {
        if (value == null) throw new IllegalArgumentException("配置不能为空");
        if (value.rules == null) value.rules = new ArrayList<>();
        if (value.bossEntities == null) value.bossEntities = new ArrayList<>();
        value.scanIntervalTicks = clamp(value.scanIntervalTicks, 5, 200);
        value.combatHoldTicks = clamp(value.combatHoldTicks, 0, 1200);
        value.fadeTicks = clamp(value.fadeTicks, 1, 200);
        value.previewVolume = (float) clamp(value.previewVolume, 0.0, 2.0);
        value.hostileRadius = clamp(value.hostileRadius, 4.0, 64.0);
        value.bossRadius = clamp(value.bossRadius, 8.0, 128.0);
        value.bossHealthThreshold = clamp(value.bossHealthThreshold, 1.0, 2048.0);
        Set<String> usedIds = new HashSet<>();
        for (int i = 0; i < value.rules.size(); i++) {
            MusicConfig.Rule rule = value.rules.get(i);
            if (rule == null) throw new IllegalArgumentException("rules[" + i + "] 不能为空");
            if (rule.id == null || rule.id.isBlank()) rule.id = "rule_" + i;
            rule.id = rule.id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
            String baseId = rule.id;
            int suffix = 2;
            while (!usedIds.add(rule.id)) rule.id = baseId + "_" + suffix++;
            if (rule.name == null || rule.name.isBlank()) rule.name = rule.id;
            if (rule.state == null) rule.state = MusicConfig.State.ANY;
            if (rule.time == null) rule.time = MusicConfig.Time.ANY;
            if (rule.bossStage == null) rule.bossStage = MusicConfig.BossStage.ANY;
            if (rule.dimensions == null) rule.dimensions = new ArrayList<>();
            if (rule.biomeIds == null) rule.biomeIds = new ArrayList<>();
            if (rule.biomeTags == null) rule.biomeTags = new ArrayList<>();
            if (rule.tracks == null) rule.tracks = new ArrayList<>();
            rule.minEnemies = Math.max(0, rule.minEnemies);
            if (rule.maxEnemies >= 0 && rule.maxEnemies < rule.minEnemies) rule.maxEnemies = rule.minEnemies;
            for (MusicConfig.Track track : rule.tracks) {
                if (track.sound == null || track.sound.isBlank()) track.sound = "minecraft:music.game";
                track.weight = Math.max(1, track.weight);
                // -1（跟随音乐库）合法；其余值钳制到 0~2
                track.volume = track.volume >= 0.0F
                        ? (float) clamp(track.volume, 0.0, 2.0)
                        : MusicConfig.Track.FOLLOW_LIBRARY;
            }
        }
    }

    private static MusicConfig createPreset(String name) {
        MusicConfig result = new MusicConfig();
        result.bossEntities.addAll(List.of("minecraft:ender_dragon", "minecraft:wither", "minecraft:elder_guardian", "minecraft:warden"));
        if ("relaxed".equals(name)) {
            result.hostileRadius = 12.0;
            result.combatHoldTicks = 60;
            result.rules.add(rule("boss", "BOSS 战", 100, MusicConfig.State.ANY, 0, -1, MusicConfig.BossStage.ENGAGED,
                    track("minecraft:music.dragon", 1, 1.0F)));
            result.rules.add(rule("night", "夜间探索", 20, MusicConfig.State.EXPLORE, 0, -1, MusicConfig.BossStage.NONE,
                    track("minecraft:music.game", 1, 0.55F)));
            result.rules.add(rule("explore", "安静探索", 10, MusicConfig.State.EXPLORE, 0, -1, MusicConfig.BossStage.NONE,
                    track("minecraft:music.overworld.forest", 2, 0.65F), track("minecraft:music.game", 1, 0.6F)));
            return result;
        }
        if ("combat".equals(name)) {
            result.hostileRadius = 20.0;
            result.combatHoldTicks = 140;
            addCombatAndBossRules(result);
            result.rules.add(rule("explore", "探索", 10, MusicConfig.State.EXPLORE, 0, -1, MusicConfig.BossStage.NONE,
                    track("minecraft:music.game", 1, 0.65F)));
            return result;
        }
        addCombatAndBossRules(result);
        MusicConfig.Rule night = rule("night_explore", "夜间探索", 15, MusicConfig.State.EXPLORE, 0, -1, MusicConfig.BossStage.NONE,
                track("minecraft:music.game", 1, 0.65F));
        night.time = MusicConfig.Time.NIGHT;
        result.rules.add(night);
        MusicConfig.Rule forest = rule("forest", "森林探索", 14, MusicConfig.State.EXPLORE, 0, -1, MusicConfig.BossStage.NONE,
                track("minecraft:music.overworld.forest", 2, 0.75F), track("minecraft:music.game", 1, 0.65F));
        forest.biomeTags.add("minecraft:is_forest");
        result.rules.add(forest);
        result.rules.add(rule("explore", "通用探索", 10, MusicConfig.State.EXPLORE, 0, -1, MusicConfig.BossStage.NONE,
                track("minecraft:music.game", 1, 0.7F)));
        return result;
    }

    private static void addCombatAndBossRules(MusicConfig result) {
        result.rules.add(rule("boss_phase_two", "BOSS 半血二阶段", 140, MusicConfig.State.COMBAT, 0, -1, MusicConfig.BossStage.PHASE_TWO,
                track("minecraft:music.dragon", 3, 1.15F), track("minecraft:music.end", 1, 1.0F)));
        result.rules.add(rule("boss_engaged", "BOSS 交战", 130, MusicConfig.State.COMBAT, 0, -1, MusicConfig.BossStage.ENGAGED,
                track("minecraft:music.dragon", 1, 1.05F)));
        result.rules.add(rule("boss_near", "BOSS 接近", 120, MusicConfig.State.ANY, 0, -1, MusicConfig.BossStage.NEAR,
                track("minecraft:music.end", 1, 0.85F)));
        result.rules.add(rule("battle_six", "激战：六名以上敌人", 90, MusicConfig.State.COMBAT, 6, -1, MusicConfig.BossStage.NONE,
                track("minecraft:music.nether.nether_wastes", 2, 1.0F), track("minecraft:music.nether.basalt_deltas", 1, 1.0F)));
        result.rules.add(rule("battle_three", "紧张：三名以上敌人", 80, MusicConfig.State.COMBAT, 3, 5, MusicConfig.BossStage.NONE,
                track("minecraft:music.nether.crimson_forest", 1, 0.9F)));
        result.rules.add(rule("combat", "锁定战斗目标", 70, MusicConfig.State.COMBAT, 1, -1, MusicConfig.BossStage.NONE,
                track("minecraft:music.nether.warped_forest", 1, 0.9F)));
        result.rules.add(rule("alert", "十六格警戒", 50, MusicConfig.State.ALERT, 1, -1, MusicConfig.BossStage.NONE,
                track("minecraft:music.overworld.deep_dark", 1, 0.72F)));
    }

    private static MusicConfig.Rule rule(String id, String name, int priority, MusicConfig.State state, int min, int max,
                                         MusicConfig.BossStage bossStage, MusicConfig.Track... tracks) {
        MusicConfig.Rule rule = new MusicConfig.Rule();
        rule.id = id;
        rule.name = name;
        rule.priority = priority;
        rule.state = state;
        rule.minEnemies = min;
        rule.maxEnemies = max;
        rule.bossStage = bossStage;
        rule.tracks.addAll(List.of(tracks));
        return rule;
    }

    private static MusicConfig.Track track(String sound, int weight, float volume) {
        return new MusicConfig.Track(sound, weight, volume, true);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
