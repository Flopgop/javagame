package net.flamgop;

import net.flamgop.gpu.buffer.BufferSerializable;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class Light implements BufferSerializable {

    public static final int BYTES = 12 * Float.BYTES;

    public Vector3f position;
    public float radius;

    public Vector3f color;
    public float constant;

    public float linear;
    public float quadratic;
    // 8 byte padding

    public Light(Vector3f position, Vector3f color, float constant, float linear, float quadratic) {
        this.position = position;
        this.color = color;
        this.constant = constant;
        this.linear = linear;
        this.quadratic = quadratic;
        this.radius = calculateRadius();
    }

    public float calculateRadius() {
        float maxBrightness = Math.max(Math.max(color.x(), color.y()), color.z());
        return (float) ((-linear + Math.sqrt(linear * linear - 4 * quadratic * (constant - (256f / 5.0f) * maxBrightness))) / 2.0f * quadratic);
    }

    @Override
    public void encode(ByteBuffer buf) {
        buf.putFloat(position.x());
        buf.putFloat(position.y());
        buf.putFloat(position.z());
        buf.putFloat(radius);
        buf.putFloat(color.x());
        buf.putFloat(color.y());
        buf.putFloat(color.z());
        buf.putFloat(constant);
        buf.putFloat(linear);
        buf.putFloat(quadratic);
        buf.putFloat(0);
        buf.putFloat(0);
    }

    @Override
    public int length() {
        return BYTES;
    }
}
