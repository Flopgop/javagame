package net.flamgop.gpu.material;

import net.flamgop.gpu.ShaderProgram;
import net.flamgop.gpu.texture.GPUTexture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Material {
    private static final Logger LOGGER = LoggerFactory.getLogger(Material.class);

    private final ShaderProgram shader;
    private final Map<Integer, GPUTexture> textures = new LinkedHashMap<>(); // linked to preserve order

    public Material(ShaderProgram shader) {
        this.shader = shader;
    }

    public void bindTexture(String shaderIdentifier, GPUTexture texture) {
        int loc = shader.getUniformLocation(shaderIdentifier);
        if (loc == -1) LOGGER.warn("Could not find texture location: {}", shaderIdentifier);
        textures.put(loc, texture);
    }

    public void bind() {
        shader.use();
        List<Map.Entry<Integer,GPUTexture>> entries = textures.entrySet().stream().toList();
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<Integer,GPUTexture> entry = entries.get(i);
            if (entry.getKey() < 0) continue; // bad
            shader.uniform1i(entry.getKey(), i);
            entry.getValue().bindToUnit(i);
        }
    }
}
