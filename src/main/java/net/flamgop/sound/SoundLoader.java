package net.flamgop.sound;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SoundLoader {
    private static final int RIFF = 0x46464952; // "RIFF"
    private static final int WAVE = 0x45564157; // "WAVE"
    private static final int FMT0 = 0x20746d66; // "fmt\0"
    private static final int DATA = 0x61746164; // "data"

    public static Sound loadWav(ByteBuffer bytes) {
        bytes.order(ByteOrder.LITTLE_ENDIAN);

        // RIFF header
        if (bytes.getInt() != RIFF) // "RIFF"
            throw new RuntimeException("WAV file is not valid (RIFF)");
        bytes.getInt(); // size
        if (bytes.getInt() != WAVE) // "WAVE"
            throw new RuntimeException("WAV file is not valid (WAVE)");

        int channels = 0;
        int sampleRate = 0;
        int bitsPerSample = 0;
        ByteBuffer audioData = null;

        while (bytes.remaining() >= 8) {
            int chunkId = bytes.getInt();
            int chunkSize = bytes.getInt();
            switch (chunkId) {
                case FMT0:
                    int audioFormat = bytes.getShort(); // PCM = 1
                    if (audioFormat != 1)
                        throw new RuntimeException("Only PCM WAV files are supported!"); // TODO: implement compressed wav loading.
                    channels = bytes.getShort();
                    sampleRate = bytes.getInt();
                    bytes.getInt(); // byte rate == (sampleRate * bitsPerSample * channels) / 8
                    bytes.getShort(); // block align == (channels * bitsPerSample) / 8
                    bitsPerSample = bytes.getShort();

                    if (chunkSize > 16) {
                        bytes.position(bytes.position() + (chunkSize - 16));
                    }
                    break;
                case DATA:
                    audioData = MemoryUtil.memAlloc(chunkSize);
                    byte[] temp = new byte[chunkSize];
                    bytes.get(temp);
                    audioData.put(temp);
                    audioData.flip();
                    break;
                default:
                    System.out.println("Sound loader encountered weird WAV chunk: 0x" + Integer.toHexString(chunkId) + ", " + chunkSize);
                    // ??
                    bytes.position(bytes.position() + chunkSize);
                    break;
            }
        }

        if (audioData == null)
            throw new RuntimeException("No audio data found in WAV file");

        return new Sound(audioData, channels, sampleRate, bitsPerSample);
    }
}
