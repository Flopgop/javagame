package net.flamgop.gpu;

import net.flamgop.util.ResourceHelper;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;

public class DefaultShaders {
    public static ShaderProgram GBUFFER;

    public static void loadDefaultShaders() {
        GBUFFER = new ShaderProgram();
        GBUFFER.attachShaderSource(ResourceHelper.loadFileContentsFromResource("gbuffer.vertex.glsl"), GL_VERTEX_SHADER);
        GBUFFER.attachShaderSource(ResourceHelper.loadFileContentsFromResource("gbuffer.fragment.glsl"), GL_FRAGMENT_SHADER);
        GBUFFER.link();
    }
}
