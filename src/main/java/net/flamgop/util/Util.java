package net.flamgop.util;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL33.glGetQueryObjectui64v;

public class Util {
    public static float[] doubleToFloatArray(double[] d) {
        float[] f = new float[d.length];
        for (int i = 0; i < d.length; i++) {
            f[i] = (float) d[i];
        }
        return f;
    }

    public static boolean isQueryReady(int query) {
        int[] available = new int[1];
        glGetQueryObjectuiv(query, GL_QUERY_RESULT_AVAILABLE, available);
        return available[0] != 0;
    }

    public static long getQueryTime(int query) {
        long[] timeNs = new long[1];
        glGetQueryObjectui64v(query, GL_QUERY_RESULT, timeNs);
        return timeNs[0];
    }
}
