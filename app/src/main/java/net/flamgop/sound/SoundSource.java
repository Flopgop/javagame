package net.flamgop.sound;

import org.joml.Vector3f;

import static org.lwjgl.openal.AL11.*;

public class SoundSource {

    private static void getSource3f(int source, int param, Vector3f out) {
        float[] px = new float[1], py = new float[1], pz = new float[1];
        alGetSource3f(source, param, px, py, pz);
        out.x = px[0];
        out.y = py[0];
        out.z = pz[0];
    }

    private final int source;

    private final Vector3f position = new Vector3f();
    private final Vector3f velocity = new Vector3f();

    private float referenceDistance = 0.0f;
    private float maxDistance = 25.0f;
    private float rolloffFactor = 1.0f;

    private float pitch = 1.0f;
    private float gain = 0.5f;
    private boolean looping = false;

    public SoundSource() {
        this.source = alGenSources();
        alSourcef(source, AL_PITCH, pitch);
        alSourcef(source, AL_GAIN, gain);
        alSource3f(source, AL_POSITION, position.x, position.y, position.z);
        alSource3f(source, AL_VELOCITY, velocity.x, velocity.y, velocity.z);
        alSourcei(source, AL_LOOPING, AL_FALSE);
    }

    public void referenceDistance(float referenceDistance) {
        this.referenceDistance = referenceDistance;
        alSourcef(source, AL_REFERENCE_DISTANCE, referenceDistance);
    }

    public float referenceDistance() {
        this.referenceDistance = alGetSourcei(source, AL_REFERENCE_DISTANCE);
        return referenceDistance;
    }

    public void maxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
        alSourcef(source, AL_MAX_DISTANCE, maxDistance);
    }

    public float maxDistance() {
        this.maxDistance = alGetSourcei(source, AL_MAX_DISTANCE);
        return maxDistance;
    }

    public void rolloffFactor(float rolloffFactor) {
        this.rolloffFactor = rolloffFactor;
        alSourcef(source, AL_ROLLOFF_FACTOR, rolloffFactor);
    }

    public float rolloffFactor() {
        this.rolloffFactor = alGetSourcei(source, AL_ROLLOFF_FACTOR);
        return rolloffFactor;
    }

    public void position(Vector3f position) {
        this.position.set(position);
        alSource3f(source, AL_POSITION, position.x, position.y, position.z);
    }

    public Vector3f position() {
        getSource3f(source, AL_POSITION, position);
        return new Vector3f(position);
    }

    public void velocity(Vector3f velocity) {
        this.velocity.set(velocity);
        alSource3f(source, AL_VELOCITY, velocity.x, velocity.y, velocity.z);
    }

    public Vector3f velocity() {
        getSource3f(source, AL_VELOCITY, velocity);
        return new Vector3f(velocity);
    }

    public void pitch(float pitch) {
        this.pitch = pitch;
        alSourcef(source, AL_PITCH, pitch);
    }

    public float pitch() {
        this.pitch = alGetSourcef(source, AL_PITCH);
        return pitch;
    }

    public void gain(float gain) {
        this.gain = gain;
        alSourcef(source, AL_GAIN, gain);
    }

    public float gain() {
        this.gain = alGetSourcef(source, AL_GAIN);
        return gain;
    }

    public void looping(boolean looping) {
        this.looping = looping;
        alSourcei(source, AL_LOOPING, looping ? AL_TRUE : AL_FALSE);
    }

    public boolean looping() {
        this.looping = alGetSourcei(source, AL_LOOPING) == AL_TRUE;
        return looping;
    }

    public void playSound(Sound sound) {
        alSourcei(source, AL_BUFFER, sound.handle());
        alSourcePlay(source);
    }

    public boolean isPlaying() {
        return alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING;
    }

    public void pauseSound() {
        alSourcePause(source);
    }

    public void stopSound() {
        alSourceStop(source);
    }

    public void destroy() {
        alDeleteSources(source);
    }
}
