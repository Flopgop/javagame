package net.flamgop.shadow;

import net.flamgop.util.AABB;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class ShadowUtil {
    public static void computeOrthoExtents(Matrix4f lightView, Matrix4f invCamViewProj,
                                           Vector3f orthoMinOut, Vector3f orthoMaxOut) {
        // specific to OpenGL; change for Vulkan or DX!!!
        Vector4f[] ndcCorners = {
                new Vector4f(-1, -1, -1, 1), new Vector4f(1, -1, -1, 1),
                new Vector4f(-1, 1, -1, 1), new Vector4f(1, 1, -1, 1),
                new Vector4f(-1, -1, 1, 1), new Vector4f(1, -1, 1, 1),
                new Vector4f(-1, 1, 1, 1), new Vector4f(1, 1, 1, 1)
        };
        Vector3f min = new Vector3f(Float.MAX_VALUE), max = new Vector3f(-Float.MAX_VALUE);

        for (Vector4f ndc : ndcCorners) {
            Vector4f world = invCamViewProj.transform(ndc, new Vector4f());
            world.div(world.w);
            Vector4f light = lightView.transform(world, new Vector4f());
            min.x = Math.min(min.x, light.x);
            min.y = Math.min(min.y, light.y);
            max.x = Math.max(max.x, light.x);
            max.y = Math.max(max.y, light.y);
        }

        orthoMinOut.set(min);
        orthoMaxOut.set(max);
    }

    // this code is like yoinked from aperture, this is LGPL licensed
    public static Vector2f computeNearAndFar(Matrix4f lightViewMatrix,
                                             Vector3f orthoMin,
                                             Vector3f orthoMax,
                                             AABB sceneAabb) {
        Vector3f[] worldCorners = sceneAabb.getCorners();
        Vector3f[] lightSpaceCorners = new Vector3f[8];
        for (int i = 0; i < 8; ++i) {
            lightSpaceCorners[i] = new Vector3f();
            lightViewMatrix.transformPosition(worldCorners[i], lightSpaceCorners[i]);
        }

        float minX = orthoMin.x;
        float maxX = orthoMax.x;
        float minY = orthoMin.y;
        float maxY = orthoMax.y;

        int[] AABB_TRIANGLES = {
                0, 1, 2, 1, 3, 2,
                4, 5, 6, 5, 6, 7,
                0, 2, 4, 2, 4, 6,
                1, 3, 5, 3, 5, 7,
                0, 1, 4, 1, 4, 5,
                2, 3, 6, 3, 6, 7
        };

        float minZ = Float.POSITIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        // clipping (yay)
        for (int t = 0; t < AABB_TRIANGLES.length; t += 3) {
            Vector3f v0 = lightSpaceCorners[AABB_TRIANGLES[t]];
            Vector3f v1 = lightSpaceCorners[AABB_TRIANGLES[t + 1]];
            Vector3f v2 = lightSpaceCorners[AABB_TRIANGLES[t + 2]];

            List<Vector3f> polygon = new ArrayList<>();
            polygon.add(new Vector3f(v0));
            polygon.add(new Vector3f(v1));
            polygon.add(new Vector3f(v2));

            for (int planeIndex = 0; planeIndex < 4; ++planeIndex) {
                if (polygon.isEmpty()) break;
                List<Vector3f> clipped = new ArrayList<>();
                float planeValue;
                int axis;
                boolean keepGreater;
                if (planeIndex == 0) {
                    planeValue = minX;
                    axis = 0;
                    keepGreater = true;
                } else if (planeIndex == 1) {
                    planeValue = maxX;
                    axis = 0;
                    keepGreater = false;
                } else if (planeIndex == 2) {
                    planeValue = minY;
                    axis = 1;
                    keepGreater = true;
                } else {
                    planeValue = maxY;
                    axis = 1;
                    keepGreater = false;
                }

                int count = polygon.size();
                for (int i = 0; i < count; ++i) {

                    Vector3f current = polygon.get(i);
                    Vector3f next = polygon.get((i + 1) % count);

                    float currentCoord = (axis == 0 ? current.x : current.y);
                    float nextCoord = (axis == 0 ? next.x : next.y);

                    boolean currentInside = keepGreater ? (currentCoord >= planeValue) : (currentCoord <= planeValue);
                    boolean nextInside = keepGreater ? (nextCoord >= planeValue) : (nextCoord <= planeValue);

                    if (currentInside) {
                        clipped.add(current);
                    }

                    if (currentInside ^ nextInside) {
                        float ts = (planeValue - currentCoord) / (nextCoord - currentCoord);
                        float ix = current.x + ts * (next.x - current.x);
                        float iy = current.y + ts * (next.y - current.y);
                        float iz = current.z + ts * (next.z - current.z);
                        clipped.add(new Vector3f(ix, iy, iz));
                    }
                }

                polygon = clipped;
            }

            for (Vector3f v : polygon) {
                if (v.z < minZ) {
                    minZ = v.z;
                }
                if (v.z > maxZ) {
                    maxZ = v.z;
                }
            }
        }

        // changed for OpenGL
        float nearDistance = -maxZ;
        float farDistance = -minZ;
        if (nearDistance < 0) nearDistance = 0.0f;
        if (farDistance < nearDistance) farDistance = nearDistance;
        return new Vector2f(nearDistance, farDistance);
    }
}
