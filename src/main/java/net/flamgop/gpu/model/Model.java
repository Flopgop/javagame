package net.flamgop.gpu.model;

import net.flamgop.Game;
import org.joml.Matrix4f;
import org.lwjgl.assimp.*;

import java.util.List;

public class Model {

    public final List<TexturedMesh> meshes;

    public Model(final List<TexturedMesh> meshes) {
        this.meshes = meshes;
    }

    public void draw(Matrix4f model) {
        for (TexturedMesh mesh : meshes) {
            if (!Game.INSTANCE.culling().isVisible(mesh, model)) continue;
            mesh.draw();
        }
    }

    public void destroy() {
        for (TexturedMesh mesh : meshes) {
            mesh.destroy();
        }
    }
}
