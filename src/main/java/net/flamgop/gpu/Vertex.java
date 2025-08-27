package net.flamgop.gpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Vertex {

    public static final int BYTES = 3 * Float.BYTES + 2 * Float.BYTES + 2 * Integer.BYTES;

    static {
        System.out.println("Vertex size is " + BYTES + " bytes.");
    }

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

    private final float[] position = new float[3];
    private final float[] texcoord = new float[2];
    private int tangent = 0;
    private int normal = 0;

    public void get(byte[] dst, int index) {
        if (dst.length < index + BYTES) throw new IndexOutOfBoundsException();

        ByteBuffer buffer = ByteBuffer.wrap(dst, index, BYTES);
        this.get(buffer);
    }

    public void get(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (float f : position) buffer.putFloat(f);
        for (float f : texcoord) buffer.putFloat(f);
        buffer.putInt(normal);
        buffer.putInt(tangent);
    }

    public void position(float x, float y, float z) {
        position[0] = x;
        position[1] = y;
        position[2] = z;
    }

    public void normal(float x, float y, float z) {
        normal = pack(x, y, z, 0);
    }

    public void texcoord(float u, float v) {
        texcoord[0] = u;
        texcoord[1] = v;
    }

    public void tangent(float x, float y, float z, int w) {
        tangent = pack(x,y,z, w > 0 ? 1 : 0);
    }
}
