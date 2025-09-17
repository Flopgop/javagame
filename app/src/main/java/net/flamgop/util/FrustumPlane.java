package net.flamgop.util;

import org.joml.Vector3f;

public record FrustumPlane(Vector3f normal, float distance) {
    public float distanceToPoint(Vector3f point) {
        return normal.dot(point) + distance;
    }

    public FrustumPlane normalized() {
        float len = normal.length();
        Vector3f newNorm = new Vector3f(normal).div(len);
        float ndist = distance / len;
        return new FrustumPlane(newNorm, ndist);
    }
}
