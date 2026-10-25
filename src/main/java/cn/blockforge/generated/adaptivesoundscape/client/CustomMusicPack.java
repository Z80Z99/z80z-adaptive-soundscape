package cn.blockforge.generated.adaptivesoundscape.client;

import cn.blockforge.generated.adaptivesoundscape.config.ConfigManager;
import cn.blockforge.generated.adaptivesoundscape.config.MusicConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 音乐库：custom_music 目录内的 .ogg/.mp3/.wav。
 * OGG 走原版声音通路（生成内置资源包），MP3/WAV 走 adaptive_lib 自有解码播放通路。
 */
public final class CustomMusicPack {
    public static final String NAMESPACE = "adaptive_custom";
    public static final String LIB_NAMESPACE = "adaptive_lib";
    private static final String PACK_ID = "adaptive_soundscape_custom_music";
    private static final Path GENERATED = ConfigManager.ROOT.resolve("generated_music_pack");
    private static final Path VOLUME_FILE = ConfigManager.ROOT.resolve("library_volumes.json");
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static Set<String> baselineOggIds = null;
    private static Set<String> currentOggIds = new HashSet<>();
    private static boolean oggSetChanged;
    private static java.util.Map<String, Float> libraryVolumes = new java.util.HashMap<>();

    public record Entry(String trackId, String relativePath, String fileName, String format, long sizeBytes) {
    }

    private CustomMusicPack() {
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(
                    PACK_ID,
                    Component.translatable("z80z_adaptive_soundscape.pack.library"),
                    true,
                    id -> new PathPackResources(id, GENERATED, false),
                    PackType.CLIENT_RESOURCES,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN
            );
            if (pack != null) consumer.accept(pack);
        });
    }

    public static synchronized int scan() {
        ENTRIES.clear();
        if (baselineOggIds == null) baselineOggIds = readExistingOggIds();
        try {
            Files.createDirectories(ConfigManager.CUSTOM_MUSIC);
            Path assets = GENERATED.resolve("assets").resolve(NAMESPACE);
            Path sounds = assets.resolve("sounds/custom");
            Files.createDirectories(sounds);
            clearOggFiles(sounds);
            JsonObject definitions = new JsonObject();
            Set<String> usedIds = new HashSet<>();
            int[] counts = {0, 0, 0};
            try (Stream<Path> files = Files.walk(ConfigManager.CUSTOM_MUSIC)) {
                files.filter(Files::isRegularFile)
                        .filter(CustomMusicPack::isAudio)
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(path -> collect(path, sounds, definitions, usedIds, counts));
            }
            Files.createDirectories(assets);
            try (Writer writer = Files.newBufferedWriter(assets.resolve("sounds.json"), StandardCharsets.UTF_8)) {
                ConfigManager.GSON.toJson(definitions, writer);
            }
            Files.writeString(GENERATED.resolve("pack.mcmeta"),
                    "{\n  \"pack\": {\"pack_format\": 15, \"description\": \"Z80Z Adaptive Soundscape music library\"}\n}\n",
                    StandardCharsets.UTF_8);
            // 基线是游戏实际加载过的 OGG 集合；与本次扫描结果不一致时才需要重载资源包
            currentOggIds = new HashSet<>(definitions.keySet());
            oggSetChanged = !currentOggIds.equals(baselineOggIds);
            lastScanCounts = new int[]{ENTRIES.size(), counts[0], counts[1], counts[2]};
        } catch (IOException exception) {
            scanFailed = exception.getMessage() == null ? "IOException" : exception.getMessage();
            lastScanCounts = null;
        }
        return ENTRIES.size();
    }

    private static int[] lastScanCounts;
    private static String scanFailed;

    /** 扫描结果文案：惰性本地化（避免启动期语言未就绪）。 */
    public static String lastScanMessage() {
        if (lastScanCounts == null) {
            return net.minecraft.client.resources.language.I18n
                    .get("message.z80z_adaptive_soundscape.scan.failed", scanFailed == null ? "" : scanFailed);
        }
        return net.minecraft.client.resources.language.I18n
                .get("message.z80z_adaptive_soundscape.scan.summary",
                        lastScanCounts[0], lastScanCounts[1], lastScanCounts[2], lastScanCounts[3]);
    }

    /**
     * 消费"OGG 集合自上次资源重载以来发生变化"的状态：返回是否需要重载，
     * 若是则把基线推进到当前集合，避免下次重复重载。
     */
    public static synchronized boolean consumeOggReloadRequired() {
        boolean required = oggSetChanged;
        if (required) {
            baselineOggIds = new HashSet<>(currentOggIds);
            oggSetChanged = false;
        }
        return required;
    }

    public static synchronized boolean isOggReloadPending() {
        return oggSetChanged;
    }

    // ===== 音乐库总音量（每个 BGM 一份，独立于 rules.json 持久化） =====

    /** 曲目在音乐库中的总音量（0~2）；未调整过返回 1.0。 */
    public static float libraryVolume(String trackId) {
        java.util.Map<String, Float> table = volumeTable();
        Float value = table.get(trackId);
        return value == null ? 1.0F : value;
    }

    public static void setLibraryVolume(String trackId, float volume) {
        volumeTable().put(trackId, Math.max(0.0F, Math.min(2.0F, volume)));
    }

    /**
     * 解析曲目最终播放音量：规则音量 ≥0 用规则独立值（覆盖）；
     * 否则跟随音乐库总音量；非音乐库曲目回落 1.0。
     */
    public static float resolveTrackVolume(String soundId, float ruleVolume) {
        if (ruleVolume >= 0.0F) return ruleVolume;
        if (soundId != null && (soundId.startsWith(NAMESPACE + ":") || soundId.startsWith(LIB_NAMESPACE + ":"))) {
            return libraryVolume(soundId);
        }
        return 1.0F;
    }

    public static void saveLibraryVolumes() {
        try {
            Files.createDirectories(ConfigManager.ROOT);
            JsonObject object = new JsonObject();
            for (java.util.Map.Entry<String, Float> entry : volumeTable().entrySet()) {
                if (entry.getValue() != null && entry.getValue() >= 0.0F) {
                    object.addProperty(entry.getKey(), entry.getValue());
                }
            }
            try (Writer writer = Files.newBufferedWriter(VOLUME_FILE, StandardCharsets.UTF_8)) {
                ConfigManager.GSON.toJson(object, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static java.util.Map<String, Float> volumeTable() {
        if (libraryVolumes == null) {
            java.util.Map<String, Float> table = new java.util.HashMap<>();
            if (Files.exists(VOLUME_FILE)) {
                try (java.io.Reader reader = Files.newBufferedReader(VOLUME_FILE, StandardCharsets.UTF_8)) {
                    JsonObject object = ConfigManager.GSON.fromJson(reader, JsonObject.class);
                    if (object != null) {
                        for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : object.entrySet()) {
                            try {
                                table.put(entry.getKey(), entry.getValue().getAsFloat());
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                } catch (IOException ignored) {
                }
            }
            libraryVolumes = table;
        }
        return libraryVolumes;
    }

    private static void forgetLibraryVolume(String trackId) {
        volumeTable().remove(trackId);
        saveLibraryVolumes();
    }

    private static Set<String> readExistingOggIds() {
        Path json = GENERATED.resolve("assets").resolve(NAMESPACE).resolve("sounds.json");
        if (!Files.exists(json)) return new HashSet<>();
        try (java.io.Reader reader = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            JsonObject object = ConfigManager.GSON.fromJson(reader, JsonObject.class);
            return object == null ? new HashSet<>() : object.keySet();
        } catch (IOException exception) {
            return new HashSet<>();
        }
    }

    public static synchronized List<Entry> libraryEntries() {
        return List.copyOf(ENTRIES);
    }

    public static String resolveLibraryFile(String trackPath) {
        String trackId = LIB_NAMESPACE + ":" + trackPath;
        for (Entry entry : ENTRIES) {
            if (entry.trackId().equals(trackId)) return entry.relativePath();
        }
        return null;
    }

    /**
     * 拖入/外部导入：校验魔数后按清洗出的安全文件名复制进音乐库目录，成功后自动重扫。
     */
    public static synchronized String importFiles(List<Path> files) {
        int imported = 0;
        int skipped = 0;
        try {
            Files.createDirectories(ConfigManager.CUSTOM_MUSIC);
            for (Path file : files) {
                String extension = extensionOf(file);
                if (extension == null || !hasValidMagic(file, extension)) {
                    skipped++;
                    continue;
                }
                String original = file.getFileName().toString();
                String base = sanitize(original.substring(0, original.length() - extension.length() - 1));
                if (base.isBlank()) {
                    skipped++;
                    continue;
                }
                Path target = uniqueTarget(base, extension);
                try {
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                    imported++;
                } catch (IOException exception) {
                    skipped++;
                }
            }
            if (imported > 0) scan();
        } catch (IOException exception) {
            return net.minecraft.client.resources.language.I18n
                    .get("message.z80z_adaptive_soundscape.import.failed",
                            exception.getMessage() == null ? "" : exception.getMessage());
        }
        if (imported == 0) {
            return net.minecraft.client.resources.language.I18n
                    .get("message.z80z_adaptive_soundscape.import.none", skipped);
        }
        return net.minecraft.client.resources.language.I18n
                .get("message.z80z_adaptive_soundscape.import.done", imported, skipped);
    }

    public static int countReferences(String trackId) {
        int count = 0;
        for (MusicConfig.Rule rule : ConfigManager.get().rules) {
            for (MusicConfig.Track track : rule.tracks) {
                if (trackId.equals(track.sound)) count++;
            }
        }
        return count;
    }

    public static synchronized boolean removeTrack(String trackId) {
        for (Entry entry : ENTRIES) {
            if (entry.trackId().equals(trackId)) {
                try {
                    Files.deleteIfExists(ConfigManager.CUSTOM_MUSIC.resolve(entry.relativePath()));
                } catch (IOException exception) {
                    return false;
                }
                forgetLibraryVolume(trackId);
                scan();
                return true;
            }
        }
        return false;
    }

    private static boolean isAudio(Path path) {
        return extensionOf(path) != null;
    }

    private static void collect(Path source, Path oggTarget, JsonObject definitions, Set<String> usedIds, int[] counts) {
        String relative = ConfigManager.CUSTOM_MUSIC.relativize(source).toString().replace('\\', '/');
        String fileName = source.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        long size;
        try {
            size = Files.size(source);
        } catch (IOException exception) {
            size = 0;
        }
        String base = sanitize(relative.substring(0, relative.length() - (fileName.length() - dot)));
        if (base.isBlank()) return;
        switch (extension) {
            case "ogg" -> {
                copyOgg(source, oggTarget, base, usedIds, definitions);
                counts[0]++;
                ENTRIES.add(new Entry(NAMESPACE + ":custom/" + base, relative, fileName, "OGG", size));
            }
            case "mp3" -> {
                counts[1]++;
                ENTRIES.add(new Entry(LIB_NAMESPACE + ":" + base, relative, fileName, "MP3", size));
            }
            default -> {
                counts[2]++;
                String format = extension.equals("aif") ? "AIFF" : extension.toUpperCase(Locale.ROOT);
                ENTRIES.add(new Entry(LIB_NAMESPACE + ":" + base, relative, fileName, format, size));
            }
        }
    }

    private static void copyOgg(Path source, Path targetDirectory, String base, Set<String> usedIds,
                                JsonObject definitions) {
        String id = base;
        int suffix = 2;
        while (!usedIds.add(id)) id = base + "_" + suffix++;
        Path target = targetDirectory.resolve(id + ".ogg");
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            JsonObject definition = new JsonObject();
            JsonArray entries = new JsonArray();
            JsonObject entry = new JsonObject();
            entry.addProperty("name", NAMESPACE + ":custom/" + id);
            entry.addProperty("stream", true);
            entries.add(entry);
            definition.add("sounds", entries);
            definitions.add("custom/" + id, definition);
        } catch (IOException ignored) {
        }
    }

    private static Path uniqueTarget(String base, String extension) {
        Path target = ConfigManager.CUSTOM_MUSIC.resolve(base + "." + extension);
        int suffix = 2;
        while (Files.exists(target)) {
            target = ConfigManager.CUSTOM_MUSIC.resolve(base + "_" + suffix++ + "." + extension);
        }
        return target;
    }

    private static String extensionOf(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        if (dot < 0) return null;
        String extension = name.substring(dot + 1);
        return SUPPORTED_EXTENSIONS.contains(extension) ? extension : null;
    }

    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("ogg", "mp3", "wav", "flac", "aiff", "aif", "au");

    private static boolean hasValidMagic(Path file, String extension) {
        try (InputStream input = Files.newInputStream(file)) {
            byte[] header = new byte[12];
            int read = input.read(header);
            if (read < 4) return false;
            switch (extension) {
                case "ogg":
                    return header[0] == 'O' && header[1] == 'g' && header[2] == 'g' && header[3] == 'S';
                case "wav":
                    return read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F'
                            && header[3] == 'F' && header[8] == 'W' && header[9] == 'A'
                            && header[10] == 'V' && header[11] == 'E';
                case "flac":
                    return header[0] == 'f' && header[1] == 'L' && header[2] == 'a' && header[3] == 'C';
                case "aiff":
                case "aif":
                    return header[0] == 'F' && header[1] == 'O' && header[2] == 'R' && header[3] == 'M';
                case "au":
                    return header[0] == '.' && header[1] == 's' && header[2] == 'n' && header[3] == 'd';
                default:
                    // mp3 / aac：ID3 头或 MPEG 帧同步
                    return (header[0] == 'I' && header[1] == 'D' && header[2] == '3')
                            || ((header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0);
            }
        } catch (IOException exception) {
            return false;
        }
    }

    static String sanitize(String base) {
        return base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
    }

    /** 从音乐库相对路径解析扩展名；未知扩展按 JavaSound 通用通路尝试。 */
    public static String libraryExtension(String relativePath) {
        String name = relativePath.toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1);
        return SUPPORTED_EXTENSIONS.contains(extension) ? extension : "wav";
    }

    public static boolean isSupportedFileName(String fileName) {
        String name = fileName.toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 && SUPPORTED_EXTENSIONS.contains(name.substring(dot + 1));
    }

    private static void clearOggFiles(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(root))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }
}
