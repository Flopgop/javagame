package net.flamgop.gpu.model;

import net.flamgop.gpu.VertexArray;

public class TexturedMesh {
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
        this.material.use();
        this.vao.draw();
    }
}
