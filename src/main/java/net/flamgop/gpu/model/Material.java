package net.flamgop.gpu.model;

import net.flamgop.gpu.DefaultShaders;
import net.flamgop.gpu.GPUTexture;
import net.flamgop.gpu.ShaderProgram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opengl.GL46.*;

public class Material {

    public static Material MISSING_MATERIAL;

    public static void loadMissingMaterial() {
        MISSING_MATERIAL = new Material(DefaultShaders.GBUFFER, GPUTexture.MISSING_TEXTURE);
    }

    private final ShaderProgram shader;
    private final List<GPUTexture> textures = new ArrayList<>();

    public Material(ShaderProgram shader, GPUTexture... textures) {
        this.shader = shader;
        this.textures.addAll(Arrays.asList(textures));
    }

    public void use() {
        for (int i = 0 ; i < textures.size() ; i++) {
            GPUTexture texture = textures.get(i);
            if (texture == null) continue; // skip this index, the texture likely doesn't exist on purpose.
            glBindTextureUnit(i, texture.handle());
        }
        shader.use();
    }
}
