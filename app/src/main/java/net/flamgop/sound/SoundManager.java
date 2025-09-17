package net.flamgop.sound;

import net.flamgop.Player;
import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.openal.ALC11.*;
import static org.lwjgl.openal.AL11.*;

public class SoundManager {

    private long device, context;
    private final ALCCapabilities alcCapabilities;
    private final ALCapabilities alCapabilities;

    private final Player player;

    private float masterGain = 1.0f;

    public SoundManager(Player player) {
        this.device = alcOpenDevice((ByteBuffer) null);
        if (this.device == 0)
            throw new RuntimeException("Failed to open ALC device");

        alcCapabilities = ALC.createCapabilities(device);

        this.context = alcCreateContext(device, (IntBuffer) null);
        if (this.context == 0)
            throw new RuntimeException("Failed to create ALC context");

        if (!alcMakeContextCurrent(this.context))
            throw new RuntimeException("Failed to make ALC context current");

        alCapabilities = AL.createCapabilities(alcCapabilities);

        this.player = player;
        masterGain(masterGain);

        alDistanceModel(AL_INVERSE_DISTANCE_CLAMPED);
    }

    public void masterGain(float gain) {
        masterGain = gain;
        alListenerf(AL_GAIN, masterGain);
    }

    public float masterGain() {
        return masterGain;
    }

    public void update() {
        Vector3f playerPosition = player.position();
        Vector3f playerVelocity = player.velocity();
        Vector3f playerForward = player.camera().forward();
        Vector3f playerUp = player.camera().up();
        alListener3f(AL_POSITION, playerPosition.x, playerPosition.y, playerPosition.z);
        alListener3f(AL_VELOCITY, playerVelocity.x, playerVelocity.y, playerVelocity.z);
        alListenerfv(AL_ORIENTATION, new float[]{playerForward.x, playerForward.y, playerForward.z, playerUp.x, playerUp.y, playerUp.z});

    }

    public void destroy() {
        alcDestroyContext(this.context);
        alcCloseDevice(this.device);
    }
}
