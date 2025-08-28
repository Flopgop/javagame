package net.flamgop.gpu.model;

import net.flamgop.gpu.VertexArray;
import net.flamgop.util.AABB;
import org.joml.Vector3f;

public class TexturedMesh {

    private static Material materialOverride = null;
    private static boolean overrideMaterial = false;

    public static void overrideMaterial(Material materialOverride) {
        TexturedMesh.materialOverride = materialOverride;
        TexturedMesh.overrideMaterial = true;
    }

    public static void disableMaterialOverride() {
        TexturedMesh.overrideMaterial = false;
    }

    private final VertexArray vao;
    private final Material material;

    private final AABB aabb;
    private final Vector3f boundingSphereCenter;
    private final float boundingSphereRadius;

    public TexturedMesh(VertexArray vao, Material material, AABB aabb, Vector3f boundingSphereCenter, float boundingSphereRadius) {
        this.vao = vao;
        this.material = material;
        this.aabb = aabb;
        this.boundingSphereCenter = boundingSphereCenter;
        this.boundingSphereRadius = boundingSphereRadius;
    }

    public AABB aabb() {
        return aabb;
    }

    public Vector3f boundingSphereCenter() {
        return boundingSphereCenter;
    }

    public float boundingSphereRadius() {
        return boundingSphereRadius;
    }

    public VertexArray vao() {
        return vao;
    }

    public void draw() {
        if (!overrideMaterial || materialOverride == null) this.material.use();
        else materialOverride.use();
        this.vao.draw();
    }

    public void destroy() {
        this.vao.destroy();
        // we don't manage our material.
    }
}
