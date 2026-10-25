package cn.blockforge.generated.adaptivesoundscape.client;

import cn.blockforge.generated.adaptivesoundscape.client.audio.Playable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class FadingMusicSound extends AbstractTickableSoundInstance implements Playable {
    private final float targetVolume;
    private final int fadeTicks;
    private boolean fadingOut;
    private int age;
    private int outAge;
    private float fadeOutStartVolume;

    public FadingMusicSound(ResourceLocation location, float targetVolume, boolean loop, int fadeTicks) {
        super(SoundEvent.createVariableRangeEvent(location), SoundSource.MUSIC, RandomSource.create());
        this.targetVolume = Math.max(0.0F, targetVolume);
        this.fadeTicks = Math.max(1, fadeTicks);
        this.looping = loop;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
        this.volume = 0.001F;
        this.pitch = 1.0F;
    }

    @Override
    public void tick() {
        age++;
        if (fadingOut) {
            outAge++;
            volume = fadeOutStartVolume * (float) Math.sqrt(Math.max(0.0, 1.0 - outAge / (double) fadeTicks));
            if (volume <= 0.001F) stop();
        } else {
            volume = Math.min(targetVolume, targetVolume * (float) Math.sqrt(age / (double) fadeTicks));
        }
    }

    public void fadeOut() {
        if (fadingOut) return;
        fadingOut = true;
        fadeOutStartVolume = volume;
        outAge = 0;
        if (fadeOutStartVolume <= 0.001F) stop();
    }

    @Override
    public boolean isActive() {
        return Minecraft.getInstance().getSoundManager().isActive(this);
    }

    @Override
    public void clientTick(float categoryVolume) {
        // 原版声音引擎自行 tick 淡入淡出并应用分类音量
    }
}
