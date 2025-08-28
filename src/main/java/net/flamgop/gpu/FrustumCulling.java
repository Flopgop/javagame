package net.flamgop.gpu;

import net.flamgop.gpu.model.TexturedMesh;
import net.flamgop.shadow.CascadedShadowMaps;
import net.flamgop.shadow.DirectionalLight;
import net.flamgop.shadow.ShadowManager;
import net.flamgop.util.AABB;
import net.flamgop.util.FrustumPlane;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class FrustumCulling {
    private final int shadowMapResolution;
    private final DirectionalLight directionalLight;
    private final ShadowManager shadowManager;
    private final Camera camera;

    private final FrustumPlane[][] cascadePlanes;
    private FrustumPlane[] planes;
    private boolean enabled = true;
    private boolean shadow = false;

    public FrustumCulling(ShadowManager shadowManager, DirectionalLight directionalLight, int shadowMapResolution, Camera camera) {
        this.shadowManager = shadowManager;
        this.directionalLight = directionalLight;
        this.shadowMapResolution = shadowMapResolution;
        this.camera = camera;
        this.planes = camera.getFrustumPlanes();
        this.cascadePlanes = new FrustumPlane[this.shadowManager.cascades()][];
        float[] splits = CascadedShadowMaps.computeCascadeSplits(this.shadowManager.cascades(), camera.near(), camera.far(), this.shadowManager.lambda());
        for (int i = 0; i < this.shadowManager.cascades(); i++) {
            cascadePlanes[i] = CascadedShadowMaps.getCascadeFrustumPlanes(camera, directionalLight, shadowMapResolution, i, splits);
        }
    }

    public void update() {
        this.planes = camera.getFrustumPlanes();
        float[] splits = CascadedShadowMaps.computeCascadeSplits(this.shadowManager.cascades(), camera.near(), camera.far(), this.shadowManager.lambda());
        for (int i = 0; i < this.shadowManager.cascades(); i++) {
            cascadePlanes[i] = CascadedShadowMaps.getCascadeFrustumPlanes(camera, directionalLight, shadowMapResolution, i, splits);
        }
    }

    public void toggle() {
        enabled = !enabled;
    }

    public void shadow(boolean shadow) {
        this.shadow = shadow;
    }

    public boolean isVisible(TexturedMesh mesh, Matrix4f model) {
        if (!enabled) return true;

        if (!shadow) {
            FrustumPlane[] planes = this.planes;
            return isMeshInsideFrustum(mesh, model, planes);
        } else {
            for (int i = 0; i < this.shadowManager.cascades(); i++) {
                FrustumPlane[] planes = this.cascadePlanes[i];
                if (isMeshInsideFrustum(mesh, model, planes)) return true;
            }
        }
        return false;
    }

    private boolean isMeshInsideFrustum(TexturedMesh mesh, Matrix4f model, FrustumPlane[] planes) {
        Vector3f center = model.transform(new Vector4f(mesh.boundingSphereCenter(), 1.0f)).xyz(new Vector3f());
        float radius = mesh.boundingSphereRadius();
        for (FrustumPlane plane : planes) {
            if (plane.distanceToPoint(center) < -radius) {
                return false;
            }
        }

        AABB aabb = mesh.aabb();
        Vector3f[] corners = aabb.getCorners();
        for (FrustumPlane plane : planes) {
            boolean allOutside = true;
            for (Vector3f corner : corners) {
                corner = model.transform(new Vector4f(corner, 1.0f)).xyz(new Vector3f());
                if (plane.distanceToPoint(corner) >= 0) {
                    allOutside = false;
                    break;
                }
            }
            if (allOutside) {
                return false;
            }
        }
        return true;
    }
}
