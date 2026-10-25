package cn.blockforge.generated.adaptivesoundscape.client;

import cn.blockforge.generated.adaptivesoundscape.client.audio.AlMusicStream;
import cn.blockforge.generated.adaptivesoundscape.client.audio.Playable;
import cn.blockforge.generated.adaptivesoundscape.config.ConfigManager;
import cn.blockforge.generated.adaptivesoundscape.config.MusicConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MusicController {
    public static final MusicController INSTANCE = new MusicController();
    private final ContextDetector detector = new ContextDetector();
    private final RuleEngine engine = new RuleEngine();
    private final List<Playable> retiring = new ArrayList<>();
    private Playable current;
    private String currentRuleId = "";
    private String currentRuleName = "";
    private String currentTrack = "";
    private ContextSnapshot snapshot;
    private int ticks;
    private int visualTicks;

    private MusicController() {
    }

    public void tick(Minecraft minecraft) {
        MusicConfig config = ConfigManager.get();
        tickPlayables(minecraft);
        if (minecraft.player == null || minecraft.level == null) {
            stop();
            return;
        }
        if (!config.enabled) {
            stop();
            return;
        }
        if (config.muteVanillaMusic) minecraft.getMusicManager().stopPlaying();
        if (PreviewPlayer.playing()) return;
        if (++ticks < config.scanIntervalTicks) return;
        ticks = 0;
        snapshot = detector.detect(minecraft, config);
        if (snapshot == null) return;
        MusicConfig.Rule rule = engine.selectRule(config, snapshot);
        if (rule == null) {
            switchTo(minecraft, null, null, config.fadeTicks, false);
            return;
        }
        boolean inactive = current == null || !current.isActive();
        if (!rule.id.equals(currentRuleId) || inactive) {
            MusicConfig.Track track = engine.selectTrack(rule, currentTrack.isEmpty() ? null : currentTrack);
            switchTo(minecraft, rule, track, config.fadeTicks, !rule.id.equals(currentRuleId));
        }
    }

    private void tickPlayables(Minecraft minecraft) {
        // 只乘 MUSIC 分类音量；主音量由 OpenAL listener 全局生效，重复叠加会导致实际音量偏低
        float volume = minecraft.options.getSoundSourceVolume(SoundSource.MUSIC);
        if (current != null) current.clientTick(volume);
        retiring.removeIf(playable -> {
            playable.clientTick(volume);
            return !playable.isActive();
        });
    }

    public void forceRefresh() {
        currentRuleId = "";
    }

    public void stop() {
        retire(current);
        current = null;
        currentRuleId = "";
        currentRuleName = "";
        currentTrack = "";
    }

    private void retire(Playable playable) {
        if (playable == null) return;
        playable.fadeOut();
        if (playable.isActive()) retiring.add(playable);
    }

    private void switchTo(Minecraft minecraft, MusicConfig.Rule rule, MusicConfig.Track track, int fadeTicks,
                          boolean announce) {
        retire(current);
        current = null;
        if (track == null) {
            currentRuleId = "";
            currentRuleName = "";
            currentTrack = "";
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(track.sound);
        if (id == null) return;
        float volume = CustomMusicPack.resolveTrackVolume(track.sound, track.volume);
        Playable playable = "adaptive_lib".equals(id.getNamespace())
                ? createLibraryStream(id, track, fadeTicks, volume)
                : createSoundEngineStream(minecraft, id, track, fadeTicks, volume);
        if (playable == null) return;
        current = playable;
        currentRuleId = rule == null ? "" : rule.id;
        currentRuleName = rule == null ? "" : rule.name;
        currentTrack = track.sound;
        if (!announce) return;
        visualTicks = 60;
        MusicConfig config = ConfigManager.get();
        if (config.noteParticles && minecraft.player != null && minecraft.level != null) {
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2.0 * i / 8.0;
                minecraft.level.addParticle(ParticleTypes.NOTE,
                        minecraft.player.getX() + Math.cos(angle) * 1.2,
                        minecraft.player.getY() + 1.0 + (i % 3) * 0.25,
                        minecraft.player.getZ() + Math.sin(angle) * 1.2,
                        i / 24.0, 0.02, 0.0);
            }
        }
    }

    private Playable createLibraryStream(ResourceLocation id, MusicConfig.Track track, int fadeTicks, float volume) {
        String reference = CustomMusicPack.resolveLibraryFile(id.getPath());
        if (reference == null) {
            MusicOverlay.announce(net.minecraft.client.resources.language.I18n
                    .get("message.z80z_adaptive_soundscape.preview.not_found", id.getPath()));
            return null;
        }
        Path file = ConfigManager.CUSTOM_MUSIC.resolve(reference);
        String extension = CustomMusicPack.libraryExtension(reference);
        AlMusicStream stream = AlMusicStream.create(file, extension, volume, track.loop, fadeTicks);
        if (stream == null) {
            MusicOverlay.announce(net.minecraft.client.resources.language.I18n
                    .get("message.z80z_adaptive_soundscape.play.cannot_decode", id.getPath()));
        }
        return stream;
    }

    private Playable createSoundEngineStream(Minecraft minecraft, ResourceLocation id, MusicConfig.Track track,
                                             int fadeTicks, float volume) {
        FadingMusicSound sound = new FadingMusicSound(id, volume, track.loop, fadeTicks);
        minecraft.getSoundManager().play(sound);
        return sound;
    }

    public void tickVisual() {
        if (visualTicks > 0) visualTicks--;
    }

    public int visualTicks() {
        return visualTicks;
    }

    public String currentRuleName() {
        return currentRuleName;
    }

    public String currentTrack() {
        return currentTrack;
    }

    public ContextSnapshot snapshot() {
        return snapshot;
    }
}
