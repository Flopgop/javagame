package net.flamgop.gpu.model;

import net.flamgop.gpu.VertexArray;

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

    public TexturedMesh(VertexArray vao, Material material) {
        this.vao = vao;
        this.material = material;
    }

    public VertexArray vao() {
        return vao;
    }

    public void draw() {
        if (!overrideMaterial || materialOverride == null) this.material.use();
        else materialOverride.use();
        this.vao.draw();
    }
}
