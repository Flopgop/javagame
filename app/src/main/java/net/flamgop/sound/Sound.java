package net.flamgop.sound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static org.lwjgl.openal.AL11.*;

public class Sound {
    private static final Logger LOGGER = LoggerFactory.getLogger(Sound.class);

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
            LOGGER.error("Don't know what format this audio is, channels = {}, bitsPerSample = {}", channels, bitsPerSample);
            return;
        }

        if (format == AL_FORMAT_STEREO8 || format == AL_FORMAT_STEREO16)
            LOGGER.warn("Audio is stereo! OpenAL doesn't support spatialized stereo audio!");

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
