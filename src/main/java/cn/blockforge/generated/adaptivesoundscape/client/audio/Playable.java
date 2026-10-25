package cn.blockforge.generated.adaptivesoundscape.client.audio;

/**
 * 音乐播放的统一抽象：原版声音引擎通路（FadingMusicSound）与自有 OpenAL 通路（AlMusicStream）共用。
 */
public interface Playable {
    void fadeOut();

    boolean isActive();

    /**
     * 每客户端 tick 由 MusicController 调用；categoryVolume 为 MUSIC 分类 × 主音量（0~1）。
     */
    void clientTick(float categoryVolume);
}
