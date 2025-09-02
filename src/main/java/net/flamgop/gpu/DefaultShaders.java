package net.flamgop.gpu;

import net.flamgop.util.ResourceHelper;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;

public class DefaultShaders {
    public static ShaderProgram GBUFFER;

    public static void loadDefaultShaders() {
        GBUFFER = new ShaderProgram();
        GBUFFER.attachShaderSource("GBuffer Vertex Shader", ResourceHelper.loadFileContentsFromResource("shaders/gbuffer.vertex.glsl"), ShaderProgram.ShaderType.VERTEX);
        GBUFFER.attachShaderSource("GBuffer Fragment Shader", ResourceHelper.loadFileContentsFromResource("shaders/gbuffer.fragment.glsl"), ShaderProgram.ShaderType.FRAGMENT);
        GBUFFER.link();
        GBUFFER.label("GBuffer Program");
    }
}
