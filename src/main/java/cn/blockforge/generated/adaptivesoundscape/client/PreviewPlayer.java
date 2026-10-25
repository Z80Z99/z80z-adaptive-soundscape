package cn.blockforge.generated.adaptivesoundscape.client;

import cn.blockforge.generated.adaptivesoundscape.client.audio.AlMusicStream;
import cn.blockforge.generated.adaptivesoundscape.client.audio.Playable;
import cn.blockforge.generated.adaptivesoundscape.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 独立试听通道：与情境状态机完全解耦——主菜单、世界内、模组关闭时均可播放，
 * 失败必然在 HUD 上给出原因，不再静默。由 ClientBootstrap 每 tick 驱动。
 */
public final class PreviewPlayer {
    private static Playable current;
    private static final List<Playable> RETIRING = new ArrayList<>();
    private static int remainingTicks;
    private static String currentName = "";
    private static boolean failureNotified;

    private PreviewPlayer() {
    }

    public static boolean playing() {
        return current != null && remainingTicks > 0 && current.isActive();
    }

    /**
     * 试听：实际响度 = 全局试听音量 previewVolume × multiplier（曲目生效音量）。
     */
    public static void play(Minecraft minecraft, String soundId, float multiplier) {
        finishCurrent();
        ResourceLocation id = ResourceLocation.tryParse(soundId);
        if (id == null) {
            MusicOverlay.announce(net.minecraft.client.resources.language.I18n
                    .get("message.z80z_adaptive_soundscape.preview.invalid_id"));
            return;
        }
        float volume = ConfigManager.get().previewVolume * Math.max(0.0F, multiplier);
        Playable playable = createPlayable(minecraft, id, volume);
        if (playable == null) return;
        MusicController.INSTANCE.stop();
        current = playable;
        currentName = soundId;
        remainingTicks = 200;
        failureNotified = false;
        MusicOverlay.announce(net.minecraft.client.resources.language.I18n
                .get("message.z80z_adaptive_soundscape.preview.playing", soundId));
    }

    private static Playable createPlayable(Minecraft minecraft, ResourceLocation id, float volume) {
        if (CustomMusicPack.LIB_NAMESPACE.equals(id.getNamespace())) {
            String reference = CustomMusicPack.resolveLibraryFile(id.getPath());
            if (reference == null) {
                MusicOverlay.announce(net.minecraft.client.resources.language.I18n
                        .get("message.z80z_adaptive_soundscape.preview.not_found", id.getPath()));
                return null;
            }
            String extension = CustomMusicPack.libraryExtension(reference);
            Path file = ConfigManager.CUSTOM_MUSIC.resolve(reference);
            AlMusicStream stream = AlMusicStream.create(file, extension, volume, false, 10);
            if (stream == null) {
                MusicOverlay.announce(net.minecraft.client.resources.language.I18n
                        .get("message.z80z_adaptive_soundscape.preview.cannot_open", reference));
                return null;
            }
            return stream;
        }
        FadingMusicSound sound = new FadingMusicSound(id, volume, false, 10);
        minecraft.getSoundManager().play(sound);
        return sound;
    }

    public static void tick(Minecraft minecraft) {
        float musicVolume = minecraft.options.getSoundSourceVolume(SoundSource.MUSIC);
        if (current != null) {
            current.clientTick(musicVolume);
            if (!failureNotified && current instanceof AlMusicStream stream && stream.hasFailed()) {
                failureNotified = true;
                MusicOverlay.announce(net.minecraft.client.resources.language.I18n
                        .get("message.z80z_adaptive_soundscape.preview.decode_failed"));
                finishCurrent();
            } else {
                remainingTicks--;
                if (remainingTicks <= 0 || !current.isActive()) finishCurrent();
            }
        }
        RETIRING.removeIf(playable -> {
            playable.clientTick(musicVolume);
            return !playable.isActive();
        });
    }

    public static void stop() {
        finishCurrent();
    }

    private static void finishCurrent() {
        if (current != null) {
            current.fadeOut();
            RETIRING.add(current);
            current = null;
        }
        remainingTicks = 0;
        currentName = "";
    }
}
