package net.flamgop;

import net.flamgop.gpu.Camera;
import net.flamgop.gpu.model.TexturedMesh;
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
    private final Camera camera;

    private FrustumPlane[] shadowPlanes;
    private FrustumPlane[] planes;
    private boolean enabled = true;
    private boolean shadow = false;

    public FrustumCulling(DirectionalLight directionalLight, int shadowMapResolution, Camera camera) {
        this.directionalLight = directionalLight;
        this.shadowMapResolution = shadowMapResolution;
        this.shadowPlanes = ShadowManager.getShadowFrustumPlanes(camera, directionalLight, shadowMapResolution);
        this.camera = camera;
        this.planes = camera.getFrustumPlanes();
    }

    public void update() {
        this.shadowPlanes = ShadowManager.getShadowFrustumPlanes(camera, directionalLight, shadowMapResolution);
        this.planes = camera.getFrustumPlanes();
    }

    public void toggle() {
        enabled = !enabled;
    }

    public void shadow(boolean shadow) {
        this.shadow = shadow;
    }

    public boolean isVisible(TexturedMesh mesh, Matrix4f model) {
        if (!enabled) return true;

        FrustumPlane[] planes = shadow ? this.shadowPlanes : this.planes;

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
