package net.flamgop;

import net.flamgop.gpu.*;
import net.flamgop.text.Font;
import net.flamgop.text.TextRenderer;
import net.flamgop.uniform.CameraUniformData;
import net.flamgop.uniform.ModelUniformData;
import net.flamgop.uniform.PBRUniformData;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.io.File;

import static org.lwjgl.opengl.GL46.*;

public class Game {

    private static final float[] FRAMEBUFFER_QUAD_VERTICES = new float[]{
            -1, 0, -1,  0, 1, 0,  0, 0, // vertex position, normal, uv
             1, 0, -1,  0, 1, 0,  1, 0,
             1, 0,  1,  0, 1, 0,  1, 1,
            -1, 0,  1,  0, 1, 0,  0, 1
    };
    private static final int[] FRAMEBUFFER_QUAD_INDICES = new int[]{
            0, 1, 2,  2, 3, 0
    };

    private final long window;

    private final ShaderProgram blit;
    private final VertexBuffer quad;
    private final GPUFramebuffer framebuffer;

    private final ShaderProgram pbr;
    private final UniformBuffer pbrUniformBuffer;
    private final UniformBuffer modelUniformBuffer;
    private final UniformBuffer cameraUniformBuffer;
    private final GPUTexture sphereTexture;
    private final ModelUniformData sphereModel;

    private final Model testModel;

    private final TextRenderer textRenderer;
    private final Font font;

    private double frameTime = 1.0f;

    public Game() {
        if (!GLFW.glfwInit()) throw new RuntimeException("Failed to initialize GLFW");

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        this.window = GLFW.glfwCreateWindow(1280, 720, "game", 0L, 0L);

        GLFW.glfwMakeContextCurrent(this.window);
        GL.createCapabilities();

        blit = new ShaderProgram();
        blit.attachShaderSource(ResourceHelper.loadFileContentsFromResource("blit.vertex.glsl"), GL_VERTEX_SHADER);
        blit.attachShaderSource(ResourceHelper.loadFileContentsFromResource("blit.fragment.glsl"), GL_FRAGMENT_SHADER);
        blit.link();

        pbr = new ShaderProgram();
        pbr.attachShaderSource(ResourceHelper.loadFileContentsFromResource("shader.vertex.glsl"), GL_VERTEX_SHADER);
        pbr.attachShaderSource(ResourceHelper.loadFileContentsFromResource("shader.fragment.glsl"), GL_FRAGMENT_SHADER);
        pbr.link();

        textRenderer = new TextRenderer();

        quad = new VertexBuffer();
        quad.data(FRAMEBUFFER_QUAD_VERTICES, 8*Float.BYTES, FRAMEBUFFER_QUAD_INDICES);
        quad.attribute(0, 3, GL_FLOAT, false, 0);
        quad.attribute(1, 3, GL_FLOAT, false, 3 * Float.BYTES);
        quad.attribute(2, 2, GL_FLOAT, false, 6 * Float.BYTES);
        framebuffer = new GPUFramebuffer(1280, 720);

        testModel = new Model();
        testModel.load("suzanne.obj");

        cameraUniformBuffer = new UniformBuffer(GPUBuffer.UpdateHint.DYNAMIC);
        modelUniformBuffer = new UniformBuffer(GPUBuffer.UpdateHint.STREAM);
        pbrUniformBuffer = new UniformBuffer(GPUBuffer.UpdateHint.STATIC);

        sphereModel = new ModelUniformData();
        sphereModel.model.identity();
        modelUniformBuffer.allocate(sphereModel);
        modelUniformBuffer.store(sphereModel);

        PBRUniformData pbr = new PBRUniformData();
        pbr.ambient = new Vector4f(ColorUtil.getRGBFromK(5900), 0.3f);
        pbr.lightColor = new Vector4f(ColorUtil.getRGBFromK(5900), 5f);
        pbr.lightDirection = new Vector3f(0f, 0.829f, -0.559f);
        pbrUniformBuffer.allocate(pbr);
        pbrUniformBuffer.store(pbr);

        CameraUniformData camera = new CameraUniformData();
        camera.position = new Vector3f(4.0f, 4.0f, 4.0f);
        camera.view.lookAt(camera.position, new Vector3f(0,0,0), new Vector3f(0,1,0));
        camera.projection.perspective((float)Math.toRadians(60f), 16f / 9f, 0.01f, 1000f);
        cameraUniformBuffer.allocate(camera);
        cameraUniformBuffer.store(camera);

        sphereTexture = GPUTexture.loadFromBytes(ResourceHelper.loadFileFromResource("cocount.jpg"));

        font = new Font(ResourceHelper.loadFileFromResource("Nunito.ttf"), 512, 1, 1024, 1024);

        font.writeAtlasToDisk(new File("font.png"));
    }

    private void start() {
        GLFW.glfwShowWindow(this.window);

        while (!this.shouldClose()) {
            double time = GLFW.glfwGetTime();
            this.update(frameTime);
            this.render(frameTime);
            this.swap();
            frameTime = GLFW.glfwGetTime() - time;
        }

        this.cleanup();
    }

    private boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(this.window);
    }

    private void update(double delta) {
        GLFW.glfwPollEvents();

        sphereModel.model.rotateY((float)delta);
        modelUniformBuffer.store(sphereModel);
    }

    private void render(double delta) {
        glClearColor(1.0f, 0.0f, 0.0f, 1.0f); // backbuffer color is bright fucking red so that we know when the framebuffer isn't drawing properly
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        framebuffer.clear(0.2f, 0.2f, 0.2f, 1.0f);

        glFrontFace(GL_CCW);
        glCullFace(GL_BACK);

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        // draw to framebuffer
        cameraUniformBuffer.bind(0);
        modelUniformBuffer.bind(1);
        pbrUniformBuffer.bind(2);
        pbr.use();
        glBindTextureUnit(0, sphereTexture.handle());
        testModel.draw(pbr);

        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);

        framebuffer.unbind();
        blit.use();
        glBindTextureUnit(0, framebuffer.texture().handle());
        quad.draw();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        textRenderer.drawText(font, String.format("FPS: %.2f", 1.0 / delta) + "\nSample text: Él jóven, naïve garçon goûta piñata\nsüßigkeiten und exclaimed, ‘¡Qué délicieux!", 25f, 75f, 0.5f, new Vector3f(0.5f, 0.8f, 0.2f));
        glDisable(GL_BLEND);
    }

    private void swap() {
        GLFW.glfwSwapBuffers(this.window);
    }

    private void cleanup() {
        GLFW.glfwDestroyWindow(this.window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("-renderdoc")) {
                System.loadLibrary("renderdoc");
            }
        }
        Game game = new Game();
        game.start();
    }
}