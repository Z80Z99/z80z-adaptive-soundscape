package cn.blockforge.generated.adaptivesoundscape.client.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 通用 JavaSound 解码：WAV/AIFF/AU（JDK 内建）+ FLAC/M4A/AAC（javasound SPI 内嵌）。
 * SPI 输出的 PCM 可能是 8/16/24/32bit、大小端、有符号/无符号或 float32，
 * JDK 的格式转换对自定义编码不可用，因此统一自行归一为 16bit PCM。
 */
final class JsaStreamDecoder implements StreamDecoder {
    private final Path file;
    private final AudioFormat format;
    private final int sampleRate;
    private final int channels;
    private final int bytesPerSample;
    private final boolean bigEndian;
    private final boolean unsigned;
    private final boolean float32;
    private AudioInputStream stream;
    private AudioInputStream converted;

    JsaStreamDecoder(Path file) throws IOException {
        this.file = file;
        this.format = probe();
        this.sampleRate = (int) format.getSampleRate();
        this.channels = format.getChannels();
        if (channels != 1 && channels != 2) {
            throw new IOException("仅支持单声道 / 立体声音频（当前 " + channels + " 声道）");
        }
        this.bigEndian = format.isBigEndian();
        this.unsigned = AudioFormat.Encoding.PCM_UNSIGNED.equals(format.getEncoding());
        this.float32 = AudioFormat.Encoding.PCM_FLOAT.equals(format.getEncoding());
        int bits = format.getSampleSizeInBits();
        if (bits <= 0 || bits % 8 != 0) {
            throw new IOException("不支持的采样位深：" + bits);
        }
        this.bytesPerSample = bits / 8;
        open();
    }

    private AudioFormat probe() throws IOException {
        try (AudioInputStream raw = AudioSystem.getAudioInputStream(file.toFile())) {
            return raw.getFormat();
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("不支持的音频格式", exception);
        }
    }

    private void open() throws IOException {
        close();
        try {
            AudioInputStream raw = AudioSystem.getAudioInputStream(file.toFile());
            // 16bit 小端 PCM 可直接透传，交给 JDK 转换最稳
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    format.getSampleRate(), 16, channels, channels * 2, format.getSampleRate(), false);
            try {
                converted = AudioSystem.getAudioInputStream(target, raw);
                stream = raw;
            } catch (IllegalArgumentException conversionUnsupported) {
                // SPI 解码流（如 FLAC 24bit）无法直转：读取原始 PCM 自行归一
                converted = null;
                stream = raw;
            }
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("音频打开失败", exception);
        }
    }

    @Override
    public int sampleRate() {
        return sampleRate;
    }

    @Override
    public int channels() {
        return channels;
    }

    @Override
    public short[] nextChunk() throws IOException {
        if (stream == null) return null;
        if (converted != null) {
            return readConverted();
        }
        int frames = 22050;
        int frameBytes = bytesPerSample * channels;
        byte[] bytes = new byte[frames * frameBytes];
        int total = 0;
        while (total < bytes.length) {
            int read = stream.read(bytes, total, bytes.length - total);
            if (read <= 0) break;
            total += read;
        }
        if (total == 0) return null;
        int completeFrames = total / frameBytes;
        short[] samples = new short[completeFrames * channels];
        int index = 0;
        for (int frame = 0; frame < completeFrames; frame++) {
            for (int channel = 0; channel < channels; channel++) {
                samples[index++] = convertSample(bytes, (frame * channels + channel) * bytesPerSample);
            }
        }
        return samples;
    }

    private short[] readConverted() throws IOException {
        byte[] bytes = new byte[22050 * 2 * channels];
        int total = 0;
        while (total < bytes.length) {
            int read = converted.read(bytes, total, bytes.length - total);
            if (read <= 0) break;
            total += read;
        }
        if (total == 0) return null;
        short[] samples = new short[total / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (short) ((bytes[i * 2] & 0xFF) | (bytes[i * 2 + 1] << 8));
        }
        return samples;
    }

    private short convertSample(byte[] bytes, int offset) {
        if (bytesPerSample == 1) {
            int value = bytes[offset];
            return (short) ((unsigned ? (value - 128) : value) << 8);
        }
        long value = 0;
        if (bigEndian) {
            for (int i = 0; i < bytesPerSample; i++) value = (value << 8) | (bytes[offset + i] & 0xFF);
        } else {
            for (int i = bytesPerSample - 1; i >= 0; i--) value = (value << 8) | (bytes[offset + i] & 0xFF);
        }
        if (float32) {
            float sample = Float.intBitsToFloat((int) value);
            sample = Math.max(-1.0F, Math.min(1.0F, sample));
            return (short) (sample * 32767.0F);
        }
        int signBit = 1 << (bytesPerSample * 8 - 1);
        if (unsigned) {
            value -= signBit;
        } else if ((value & signBit) != 0) {
            value -= signBit << 1;
        }
        int shift = bytesPerSample * 8 - 16;
        if (shift > 0) value >>= shift;
        else if (shift < 0) value <<= -shift;
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
    }

    @Override
    public void rewind() {
        try {
            open();
        } catch (IOException exception) {
            stream = null;
        }
    }

    @Override
    public void close() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
        stream = null;
        converted = null;
    }
}
