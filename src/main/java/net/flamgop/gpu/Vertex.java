package net.flamgop.gpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Vertex {

    public static final int BYTES = 5 * Float.BYTES + 2 * Integer.BYTES;

    private static int pack10(float f) {
        f = Math.max(-1.0f, Math.min(1.0f, f));
        return Math.round(f * 511f); // 10bits
    }

    private static int pack(float nx, float ny, float nz, int w) {
        int x = pack10(nx);
        int y = pack10(ny);
        int z = pack10(nz);
        w = w & 0x3; // 2 bits

        return (w << 30) | (z << 20) | (y << 10) | x;
    }

    private float x, y, z;
    private float u, v;
    private int tangent = 0;
    private int normal = 0;

    public void get(byte[] dst, int index) {
        if (dst.length < index + BYTES) throw new IndexOutOfBoundsException();

        ByteBuffer buffer = ByteBuffer.wrap(dst, index, BYTES);
        this.get(buffer);
    }

    public void get(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putFloat(z);
        buffer.putFloat(u);
        buffer.putFloat(v);
        buffer.putInt(normal);
        buffer.putInt(tangent);
    }

    public void position(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void normal(float x, float y, float z) {
        normal = pack(x, y, z, 0);
    }

    public void texcoord(float u, float v) {
        this.u = u;
        this.v = v;
    }

    public void tangent(float x, float y, float z, int w) {
        tangent = pack(x,y,z, w > 0 ? 1 : 0);
    }
}
