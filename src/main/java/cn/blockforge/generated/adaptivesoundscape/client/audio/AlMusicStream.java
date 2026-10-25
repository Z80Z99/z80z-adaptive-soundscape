package cn.blockforge.generated.adaptivesoundscape.client.audio;

import com.mojang.logging.LogUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 自有 OpenAL 播放通路：后台线程纯 Java 解码（MP3/WAV），渲染线程做全部 AL 操作，
 * 流式喂数据 + 等功率淡入淡出 + MUSIC 分类音量跟随（主音量由 listener 全局生效）。
 */
public final class AlMusicStream implements Playable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int AL_BUFFERS = 8;

    private final StreamDecoder decoder;
    private final String name;
    private final Thread decodeThread;
    private final BlockingQueue<short[]> pending = new ArrayBlockingQueue<>(16);
    private final Queue<Integer> freeBuffers = new ArrayDeque<>();
    private final int[] buffers = new int[AL_BUFFERS];
    private final int source;
    private final float targetVolume;
    private final int fadeTicks;
    private final boolean loop;
    private volatile boolean stopDecoder;
    private volatile boolean decoderEof;
    private volatile boolean failed;
    private volatile boolean fadeOutRequested;
    private volatile boolean released;
    private volatile boolean fadeDone;
    private volatile float categoryVolume = 1.0F;
    private int age;
    private int outAge;
    private float lastGain;
    private float fadeOutStartGain;

    private AlMusicStream(StreamDecoder decoder, String name, float volume, boolean loop, int fadeTicks) {
        this.decoder = decoder;
        this.name = name;
        this.targetVolume = Math.max(0.0F, volume);
        this.loop = loop;
        this.fadeTicks = Math.max(1, fadeTicks);
        this.source = AL10.alGenSources();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer handle = stack.mallocInt(AL_BUFFERS);
            for (int i = 0; i < AL_BUFFERS; i++) {
                handle.put(i, AL10.alGenBuffers());
                buffers[i] = handle.get(i);
                freeBuffers.add(buffers[i]);
            }
        }
        AL10.alSourcef(source, AL10.AL_GAIN, 0.001F);
        this.decodeThread = new Thread(this::decodeLoop, "adaptive-soundscape-decoder");
        this.decodeThread.setDaemon(true);
        this.decodeThread.start();
    }

    /**
     * 打开失败（文件损坏/格式不支持）返回 null，由调用方提示。
     */
    public static AlMusicStream create(Path file, String extension, float volume, boolean loop, int fadeTicks) {
        try {
            StreamDecoder decoder = "mp3".equals(extension)
                    ? new Mp3StreamDecoder(file)
                    : new JsaStreamDecoder(file);
            return new AlMusicStream(decoder, file.getFileName().toString(), volume, loop, fadeTicks);
        } catch (Throwable throwable) {
            LOGGER.error("打开音频失败：{}", file, throwable);
            return null;
        }
    }

    public boolean hasFailed() {
        return failed;
    }

    private void decodeLoop() {
        try {
            while (!stopDecoder) {
                short[] chunk = decoder.nextChunk();
                if (chunk == null) {
                    if (loop) {
                        decoder.rewind();
                        continue;
                    }
                    decoderEof = true;
                    return;
                }
                pending.put(chunk);
            }
        } catch (InterruptedException expected) {
            Thread.currentThread().interrupt();
        } catch (Throwable throwable) {
            failed = true;
            LOGGER.error("音频解码线程失败：{}", name, throwable);
        } finally {
            decoder.close();
        }
    }

    @Override
    public void clientTick(float volume) {
        if (released) return;
        categoryVolume = volume;
        recycleBuffers();
        fillBuffers();
        boolean drained = decoderEof && pending.isEmpty()
                && AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED) == 0;
        if (!drained && AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
            AL10.alSourcePlay(source);
        }
        tickGain();
        if (fadeDone || drained || failed) release();
    }

    private void recycleBuffers() {
        int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
        while (processed-- > 0) {
            int buffer = AL10.alSourceUnqueueBuffers(source);
            if (buffer != 0) freeBuffers.add(buffer);
        }
    }

    private void fillBuffers() {
        while (!freeBuffers.isEmpty()) {
            short[] chunk = pending.poll();
            if (chunk == null || chunk.length == 0) break;
            int buffer = freeBuffers.poll();
            ByteBuffer data = ByteBuffer.allocateDirect(chunk.length * 2).order(ByteOrder.nativeOrder());
            data.asShortBuffer().put(chunk);
            int format = decoder.channels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            AL10.alBufferData(buffer, format, data, decoder.sampleRate());
            AL10.alSourceQueueBuffers(source, buffer);
        }
    }

    private void tickGain() {
        if (fadeOutRequested) {
            outAge++;
            float gain = fadeOutStartGain * (float) Math.sqrt(Math.max(0.0, 1.0 - outAge / (double) fadeTicks));
            if (gain <= 0.001F) fadeDone = true;
            lastGain = gain;
        } else {
            age++;
            lastGain = Math.min(targetVolume, targetVolume * (float) Math.sqrt(age / (double) fadeTicks));
        }
        AL10.alSourcef(source, AL10.AL_GAIN, lastGain * categoryVolume);
    }

    @Override
    public void fadeOut() {
        if (fadeOutRequested || released) return;
        fadeOutRequested = true;
        fadeOutStartGain = Math.max(lastGain, 0.001F);
        outAge = 0;
    }

    @Override
    public boolean isActive() {
        return !released && !fadeDone && !failed;
    }

    private void release() {
        if (released) return;
        released = true;
        stopDecoder = true;
        decodeThread.interrupt();
        AL10.alSourceStop(source);
        AL10.alSourcei(source, AL10.AL_BUFFER, 0);
        AL10.alDeleteSources(source);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer handle = stack.mallocInt(AL_BUFFERS);
            handle.put(buffers);
            handle.flip();
            AL10.alDeleteBuffers(handle);
        }
        pending.clear();
    }
}
