package net.flamgop.shadow;

import net.flamgop.Game;
import net.flamgop.gpu.Camera;
import net.flamgop.util.AABB;
import net.flamgop.util.FrustumPlane;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

public class CascadedShadowMaps {
    public static float[] computeCascadeSplits(int cascadeCount, float near, float far, float lambda) {
        float[] splits = new float[cascadeCount + 1];
        splits[0] = near;
        for (int i = 1; i <= cascadeCount; i++) {
            float fraction = (float) i / (float) cascadeCount;
            float log = (float) (near * Math.pow(far / near, fraction));
            float lin = near + (far - near) * fraction;
            splits[i] = lambda * log + (1.0f - lambda) * lin;
        }
        return splits;
    }

    public static Matrix4f[] computeCascadeMatrices(Camera camera, Matrix4f lightView, int cascadeCount, int resolution, float[] splits) {
        Matrix4f[] result = new Matrix4f[cascadeCount];

        for (int i = 0; i < cascadeCount; i++) {
            result[i] = computeCascadeMatrix(i, camera, lightView, resolution, splits);
        }

        return result;
    }

    public static Matrix4f computeCascadeMatrix(int cascade, Camera camera, Matrix4f lightView, int resolution, float[] splits) {
        float cascadeNear = splits[cascade];
        float cascadeFar = splits[cascade + 1];
        Matrix4f cascadeProj = new Matrix4f().perspective(camera.fov(), camera.aspect(), cascadeNear, cascadeFar, true);
        Matrix4f invCamViewProj = new Matrix4f(cascadeProj).mul(camera.view()).invert();
        Vector3f orthoMin = new Vector3f(), orthoMax = new Vector3f();
        ShadowUtil.computeOrthoExtents(lightView, invCamViewProj, orthoMin, orthoMax);

        Vector3f min = new Vector3f(), max = new Vector3f();
        invCamViewProj.frustumAabb(min, max);
        Vector2f nearFar = ShadowUtil.computeNearAndFar(lightView, orthoMin, orthoMax, new AABB(min, max));

        float lightNear = nearFar.x;
        Vector3f tmp = new Vector3f();
        List<AABB> aabbs = Game.INSTANCE.level().getAllObjectBounds();
        for (AABB aabb : aabbs) {
            Vector3f[] corners = aabb.getCorners();
            for (Vector3f corner : corners) {
                tmp.set(corner).mulPosition(lightView);
                lightNear = Math.min(lightNear, tmp.z);
            }
        }

        snapOrthoToTexels(orthoMin, orthoMax, resolution);

        Matrix4f ortho = new Matrix4f().ortho(orthoMin.x, orthoMax.x, orthoMin.y, orthoMax.y, lightNear, nearFar.y);
        return new Matrix4f(ortho).mul(lightView);
    }

    public static FrustumPlane[] getCascadeFrustumPlanes(Camera camera, DirectionalLight light, int resolution, int cascade, float[] splits) {
        Matrix4f viewProj = computeCascadeMatrix(cascade, camera, ShadowManager.getShadowView(light), resolution, splits);

        float m00 = viewProj.m00(), m01 = viewProj.m01(), m02 = viewProj.m02(), m03 = viewProj.m03();
        float m10 = viewProj.m10(), m11 = viewProj.m11(), m12 = viewProj.m12(), m13 = viewProj.m13();
        float m20 = viewProj.m20(), m21 = viewProj.m21(), m22 = viewProj.m22(), m23 = viewProj.m23();
        float m30 = viewProj.m30(), m31 = viewProj.m31(), m32 = viewProj.m32(), m33 = viewProj.m33();

        return new FrustumPlane[]{
                new FrustumPlane(new Vector3f(m03 + m00, m13 + m10, m23 + m20), m33 + m30).normalized(), // Left
                new FrustumPlane(new Vector3f(m03 - m00, m13 - m10, m23 - m20), m33 - m30).normalized(), // Right
                new FrustumPlane(new Vector3f(m03 + m01, m13 + m11, m23 + m21), m33 + m31).normalized(), // Bottom
                new FrustumPlane(new Vector3f(m03 - m01, m13 - m11, m23 - m21), m33 - m31).normalized(), // Top
                new FrustumPlane(new Vector3f(m03 + m02, m13 + m12, m23 + m22), m33 + m32).normalized(), // Near
                new FrustumPlane(new Vector3f(m03 - m02, m13 - m12, m23 - m22), m33 - m32).normalized()  // Far
        };
    }

    private static void snapOrthoToTexels(Vector3f min, Vector3f max, int resolution) {
        float width = max.x - min.x;
        float height = max.y - min.y;

        float texelSizeX = width / (float) resolution;
        float texelSizeY = height / (float) resolution;

        float centerX = (min.x + max.x) * 0.5f;
        float centerY = (min.y + max.y) * 0.5f;

        float snappedCenterX = (float) (Math.floor(centerX / texelSizeX) * texelSizeX);
        float snappedCenterY = (float) (Math.floor(centerY / texelSizeY) * texelSizeY);

        float halfW = width * 0.5f;
        float halfH = height * 0.5f;

        min.x = snappedCenterX - halfW;
        max.x = snappedCenterX + halfW;
        min.y = snappedCenterY - halfH;
        max.y = snappedCenterY + halfH;
    }
}
