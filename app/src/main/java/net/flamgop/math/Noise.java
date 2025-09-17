package net.flamgop.math;

// this class uses a cheat for cheap noise
public class Noise {

    private static final float A = 1.41421356237f;
    private static final float B = 1.73205080757f;
    private static final float C = 2.23606797755f;
    private static final float D = 2.44948974278f;
    private static final float E = 2.64575131106f;
    private static final float F = 2.82842712475f;
    private static final float G = 3.16227766017f;

    private static final float H = 3.31662479036f;
    private static final float I = 3.46410161514f;
    private static final float J = 3.60555127546f;
    private static final float K = 3.74165738677f;
    private static final float L = 3.87298334621f;
    private static final float M = 4.12310562562f;
    private static final float N = 4.24264068712f;

    private static final float[] FREQUENCIES_X = new float[]{
            A,I,C,K
    };

    private static final float[] FREQUENCIES_Y = new float[]{
            H,B,J,D
    };

    public static float cheap(float x) {
        return (float) (Math.sin(x) + Math.sin(A * x) + Math.sin(B * x) + Math.sin(C * x) + Math.sin(D * x) + Math.sin(E * x) + Math.sin(F * x) + Math.sin(G * x));
    }

    public static float cheap(float x, float y) {
        y *= 10f;

        float sum = 0f;
        float amplitude = 1f;
        for (final float xFreq : FREQUENCIES_X) {
            for (final float yFreq : FREQUENCIES_Y) {
                float nx = x * xFreq + 10f * xFreq;
                float ny = y * yFreq + 10f * yFreq;

                float theta = xFreq * yFreq;
                nx = (float) (nx * Math.cos(theta) - ny * Math.sin(theta));
                ny = (float) (nx * Math.sin(theta) + ny * Math.cos(theta));

                sum += (float) (Math.sin(nx / 15f) + Math.sin(ny) * amplitude);
            }
        }
        return sum / (FREQUENCIES_X.length * FREQUENCIES_Y.length);
    }
}
