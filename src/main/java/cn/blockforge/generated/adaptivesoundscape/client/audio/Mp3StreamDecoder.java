package cn.blockforge.generated.adaptivesoundscape.client.audio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

final class Mp3StreamDecoder implements StreamDecoder {
    private final Path file;
    private Bitstream stream;
    private Decoder decoder;
    private int sampleRate = 44100;
    private int channels = 2;

    Mp3StreamDecoder(Path file) throws IOException {
        this.file = file;
        open();
    }

    private void open() throws IOException {
        InputStream input = new BufferedInputStream(Files.newInputStream(file), 1 << 16);
        stream = new Bitstream(input);
        decoder = new Decoder();
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
        short[] chunk = new short[0];
        int length = 0;
        int target = 22050 * channels;
        while (length < target) {
            Header header;
            try {
                header = stream.readFrame();
            } catch (BitstreamException exception) {
                throw new IOException("MP3 流读取失败", exception);
            }
            if (header == null) {
                return length == 0 ? null : resize(chunk, length);
            }
            try {
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, stream);
                stream.closeFrame();
                sampleRate = output.getSampleFrequency();
                channels = output.getChannelCount();
                short[] samples = output.getBuffer();
                int count = output.getBufferLength();
                if (length + count > chunk.length) {
                    chunk = resize(chunk, Math.max(length + count, Math.max(1024, chunk.length) * 2));
                }
                System.arraycopy(samples, 0, chunk, length, count);
                length += count;
            } catch (DecoderException exception) {
                throw new IOException("MP3 解码失败", exception);
            }
        }
        return resize(chunk, length);
    }

    private static short[] resize(short[] buffer, int size) {
        short[] result = new short[size];
        System.arraycopy(buffer, 0, result, 0, Math.min(size, buffer.length));
        return result;
    }

    @Override
    public void rewind() {
        close();
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
            } catch (BitstreamException ignored) {
            }
        }
    }
}
