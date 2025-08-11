package net.flamgop.util;

import org.joml.Vector3f;

public record AABB(Vector3f min, Vector3f max) {
    public AABB(Vector3f min, Vector3f max) {
        this.min = new Vector3f(min);
        this.max = new Vector3f(max);
    }

    public Vector3f[] getCorners() {
        Vector3f[] corners = new Vector3f[8];
        float minX = min.x, minY = min.y, minZ = min.z;
        float maxX = max.x, maxY = max.y, maxZ = max.z;
        // least deranged code
        corners[0] = new Vector3f(minX, minY, minZ);
        corners[1] = new Vector3f(maxX, minY, minZ);
        corners[2] = new Vector3f(minX, maxY, minZ);
        corners[3] = new Vector3f(maxX, maxY, minZ);
        corners[4] = new Vector3f(minX, minY, maxZ);
        corners[5] = new Vector3f(maxX, minY, maxZ);
        corners[6] = new Vector3f(minX, maxY, maxZ);
        corners[7] = new Vector3f(maxX, maxY, maxZ);
        return corners;
    }
}
