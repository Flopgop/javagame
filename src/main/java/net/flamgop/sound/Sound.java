package net.flamgop.sound;

import java.nio.ByteBuffer;

import static org.lwjgl.openal.AL11.*;

public class Sound {
    private boolean valid;
    private final int buffer;

    public Sound(ByteBuffer soundData, int channels, int sampleRate, int bitsPerSample) {
        this.buffer = alGenBuffers();

        int format;
        if (channels == 1 && bitsPerSample == 8)
            format = AL_FORMAT_MONO8;
        else if (channels == 1 && bitsPerSample == 16)
            format = AL_FORMAT_MONO16;
        else if (channels == 2 && bitsPerSample == 8)
            format = AL_FORMAT_STEREO8;
        else if (channels == 2 && bitsPerSample == 16)
            format = AL_FORMAT_STEREO16;
        else {
            this.valid = false;
            System.out.println("Don't know what format this audio is, channels = " + channels + ", bitsPerSample = " + bitsPerSample);
            return;
        }

        if (format == AL_FORMAT_STEREO8 || format == AL_FORMAT_STEREO16)
            System.out.println("Audio is stereo! OpenAL doesn't support spatialized stereo audio!");

        alBufferData(buffer, format, soundData, sampleRate);
        this.valid = true;
    }

    public boolean valid() {
        return valid;
    }

    public int handle() {
        if (!this.valid) return 0;
        return buffer;
    }

    public void destroy() {
        if (!this.valid) throw new RuntimeException(this + " is not valid!");
        alDeleteBuffers(buffer);
        this.valid = false;
    }
}
