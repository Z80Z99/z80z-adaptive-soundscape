package cn.blockforge.generated.adaptivesoundscape.client.audio;

import java.io.IOException;

/**
 * 后台解码线程使用的流式解码器：每次返回约 0.5 秒的 16bit PCM。
 * sampleRate/channels 在首个 nextChunk 之后才保证有效。
 */
interface StreamDecoder {
    int sampleRate();

    int channels();

    short[] nextChunk() throws IOException;

    void rewind();

    void close();
}
