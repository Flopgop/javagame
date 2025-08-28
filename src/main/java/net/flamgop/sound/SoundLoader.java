package net.flamgop.sound;

import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;

public class SoundLoader {

    public static int asciiToInt(String s, boolean bigEndian) {
        if (s.length() != 4) {
            throw new IllegalArgumentException("String must be exactly 4 characters long.");
        }

        byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length != 4) {
            throw new IllegalArgumentException("Encoding must produce exactly 4 bytes.");
        }

        int result = 0;
        if (bigEndian) {
            // [char0][char1][char2][char3]
            result |= (bytes[0] & 0xFF) << 24;
            result |= (bytes[1] & 0xFF) << 16;
            result |= (bytes[2] & 0xFF) << 8;
            result |= (bytes[3] & 0xFF);
        } else {
            // little endian: reversed order
            result |= (bytes[3] & 0xFF) << 24;
            result |= (bytes[2] & 0xFF) << 16;
            result |= (bytes[1] & 0xFF) << 8;
            result |= (bytes[0] & 0xFF);
        }

        return result;
    }

    // WAV
    private static final int RIFF = asciiToInt("RIFF", false);
    private static final int WAVE = asciiToInt("WAVE", false);
    private static final int FMT0 = asciiToInt("fmt ", false);
    private static final int DATA = asciiToInt("data", false);

    // *.wav
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
            if (chunkId == FMT0) {
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
                continue;
            }
            if (chunkId == DATA) {
                audioData = MemoryUtil.memAlloc(chunkSize);
                byte[] temp = new byte[chunkSize];
                bytes.get(temp);
                audioData.put(temp);
                audioData.flip();
                continue;
            }
            System.out.println("Sound loader encountered weird WAV chunk: 0x" + Integer.toHexString(chunkId) + ", " + chunkSize);
            bytes.position(bytes.position() + chunkSize);
        }

        if (audioData == null)
            throw new RuntimeException("No audio data found in WAV file");

        return new Sound(audioData, channels, sampleRate, bitsPerSample);
    }

    // *.ogg
    public static Sound loadOGG(ByteBuffer bytes) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pChannels = stack.mallocInt(1);
            IntBuffer pSampleRate = stack.mallocInt(1);

            ShortBuffer pcm = STBVorbis.stb_vorbis_decode_memory(bytes, pChannels, pSampleRate);
            if (pcm == null) {
                // todo: is there a reason why?
                throw new RuntimeException("STBVorbis failed to decode OGG.");
            }

            int channels = pChannels.get(0);
            int sampleRate = pSampleRate.get(0);
            int bitsPerSample = 16; // STBVorbis always outputs 16 bit PCM

            ByteBuffer audioData = MemoryUtil.memAlloc(pcm.limit() * 2);
            while (pcm.hasRemaining()) {
                audioData.putShort(pcm.get());
            }
            audioData.flip();
            return new Sound(audioData, channels, sampleRate, bitsPerSample);
        }
    }
}
