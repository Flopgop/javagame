package net.flamgop;

import net.flamgop.gpu.*;
import net.flamgop.input.InputSequenceHandler;
import net.flamgop.input.InputState;
import net.flamgop.text.Font;
import net.flamgop.text.TextRenderer;
import net.flamgop.uniform.ModelUniformData;
import net.flamgop.uniform.PBRUniformData;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.IntBuffer;

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

//    private final ShaderProgram blit;
    private final VertexBuffer quad;
    private final GPUFramebuffer framebuffer;

    private final ShaderProgram gbuffer;
    private final ShaderProgram gbufferBlit;
    private GPUTexture gbufferPositionTexture;
    private GPUTexture gbufferNormalTexture;
    private GPUTexture gbufferColorTexture;

//    private final ShaderProgram pbr;
    private final UniformBuffer pbrUniformBuffer;
    private final UniformBuffer modelUniformBuffer;
    private final GPUTexture sphereTexture;
    private final ModelUniformData sphereModel;

    private final Model testModel;

    private final TextRenderer textRenderer;
    private final Font font;

    private double frameTime = 1.0f;

    private static final double FRAMERATE_UPDATE_RATE = 1f;
    private double framerate = 1 / frameTime;
    private double framerateUpdateCounter = FRAMERATE_UPDATE_RATE;

    private final Camera camera;
    private final InputSequenceHandler inputSequenceHandler;
    private final InputState inputState;

    private float speed = 2.0f;
    private int width, height;

    public Game() {
        if (!GLFW.glfwInit()) throw new RuntimeException("Failed to initialize GLFW");

        this.width = 1280;
        this.height = 720;

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        this.window = GLFW.glfwCreateWindow(width, height, "game", 0L, 0L);

        this.inputSequenceHandler = new InputSequenceHandler();
        this.inputState = new InputState();

        //noinspection resource
        GLFW.glfwSetKeyCallback(window, (_, key, _, action, mods) -> {
            inputSequenceHandler.handleKey(key, mods, action);
            inputState.handleKey(key, action);
        });

        GLFW.glfwMakeContextCurrent(this.window);
        GL.createCapabilities();

//        blit = new ShaderProgram();
//        blit.attachShaderSource(ResourceHelper.loadFileContentsFromResource("blit.vertex.glsl"), GL_VERTEX_SHADER);
//        blit.attachShaderSource(ResourceHelper.loadFileContentsFromResource("blit.fragment.glsl"), GL_FRAGMENT_SHADER);
//        blit.link();
//
//        pbr = new ShaderProgram();
//        pbr.attachShaderSource(ResourceHelper.loadFileContentsFromResource("shader.vertex.glsl"), GL_VERTEX_SHADER);
//        pbr.attachShaderSource(ResourceHelper.loadFileContentsFromResource("shader.fragment.glsl"), GL_FRAGMENT_SHADER);
//        pbr.link();

        gbuffer = new ShaderProgram();
        gbuffer.attachShaderSource(ResourceHelper.loadFileContentsFromResource("gbuffer.vertex.glsl"), GL_VERTEX_SHADER);
        gbuffer.attachShaderSource(ResourceHelper.loadFileContentsFromResource("gbuffer.fragment.glsl"), GL_FRAGMENT_SHADER);
        gbuffer.link();

        gbufferBlit = new ShaderProgram();
        gbufferBlit.attachShaderSource(ResourceHelper.loadFileContentsFromResource("gbuffer_blit.vertex.glsl"), GL_VERTEX_SHADER);
        gbufferBlit.attachShaderSource(ResourceHelper.loadFileContentsFromResource("gbuffer_blit.fragment.glsl"), GL_FRAGMENT_SHADER);
        gbufferBlit.link();

        gbufferBlit.use();
        glUniform1i(gbufferBlit.getUniformLocation("gbuffer_position"), 0);
        glUniform1i(gbufferBlit.getUniformLocation("gbuffer_normal"), 1);
        glUniform1i(gbufferBlit.getUniformLocation("gbuffer_color"), 2);
        
        textRenderer = new TextRenderer(width, height);

        quad = new VertexBuffer();
        quad.data(FRAMEBUFFER_QUAD_VERTICES, 8*Float.BYTES, FRAMEBUFFER_QUAD_INDICES);
        quad.attribute(0, 3, GL_FLOAT, false, 0);
        quad.attribute(1, 3, GL_FLOAT, false, 3 * Float.BYTES);
        quad.attribute(2, 2, GL_FLOAT, false, 6 * Float.BYTES);

        framebuffer = new GPUFramebuffer(width, height, (fb, w, h) -> {
            gbufferPositionTexture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
            gbufferPositionTexture.storage(1, GL_RGBA16F, w, h);
            glTextureParameteri(gbufferPositionTexture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(gbufferPositionTexture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            gbufferNormalTexture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
            gbufferNormalTexture.storage(1, GL_RGBA16F, w, h);
            glTextureParameteri(gbufferNormalTexture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(gbufferNormalTexture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            gbufferColorTexture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
            gbufferColorTexture.storage(1, GL_RGBA8, w, h);
            glTextureParameteri(gbufferColorTexture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(gbufferColorTexture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            fb.texture(gbufferPositionTexture, GL_COLOR_ATTACHMENT0, 0);
            fb.texture(gbufferNormalTexture, GL_COLOR_ATTACHMENT1, 0);
            fb.texture(gbufferColorTexture, GL_COLOR_ATTACHMENT2, 0);

            fb.renderbuffer(GL_DEPTH24_STENCIL8, GL_DEPTH_STENCIL_ATTACHMENT, w, h);

            fb.drawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2});
        });

        testModel = new Model();
        testModel.load("suzanne.obj");

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

        camera = new Camera(
                new Vector3f(4.0f, 4.0f, 4.0f),
                new Vector3f(0.0f),
                new Vector3f(0f, 1f, 0f),
                (float) Math.toRadians(60f),
                (float) width / height,
                0.01f,
                1000f
        );

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

    private void resize(int width, int height) {
        this.width = width;
        this.height = height;
        this.camera.resize(width, height);
        this.framebuffer.resize(width, height);
        this.textRenderer.resize(width, height);
    }

    private void update(double delta) {
        GLFW.glfwPollEvents();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.callocInt(1);
            IntBuffer pHeight = stack.callocInt(1);
            GLFW.glfwGetWindowSize(window, pWidth, pHeight);
            int width = pWidth.get(0);
            int height = pHeight.get(0);
            if (width != this.width || height != this.height) {
                resize(width, height);
            }
        }

        framerateUpdateCounter -= delta;
        if (framerateUpdateCounter < 0) {
            framerateUpdateCounter = FRAMERATE_UPDATE_RATE;
            framerate = 1 / delta;
        }

        Vector3f camPos = camera.position();
        Vector3f targetPos = camera.target();
        Vector3f forward = camera.forward().mul((float)delta * speed);
        Vector3f right = camera.right().mul((float)delta * speed);

        boolean cameraUpdated = false;
        if (inputState.isKeyDown(GLFW.GLFW_KEY_W)) {
            camPos.add(forward);
            targetPos.add(forward);
            cameraUpdated = true;
        }
        if (inputState.isKeyDown(GLFW.GLFW_KEY_S)) {
            camPos.sub(forward);
            targetPos.sub(forward);
            cameraUpdated = true;
        }
        if (inputState.isKeyDown(GLFW.GLFW_KEY_D)) {
            camPos.add(right);
            targetPos.add(right);
            cameraUpdated = true;
        }
        if (inputState.isKeyDown(GLFW.GLFW_KEY_A)) {
            camPos.sub(right);
            targetPos.sub(right);
            cameraUpdated = true;
        }

        if (cameraUpdated) {
            camera.position(camPos);
            camera.target(targetPos);
            camera.reconfigureView();
        }

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
        camera.bind(0);
        modelUniformBuffer.bind(1);
//        pbrUniformBuffer.bind(2);
//        pbr.use();
        gbuffer.use();
        glBindTextureUnit(0, sphereTexture.handle());
        testModel.draw(gbuffer);

        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);

        framebuffer.unbind();
        camera.bind(0);
        pbrUniformBuffer.bind(1);
        gbufferBlit.use();
        glBindTextureUnit(0, gbufferPositionTexture.handle());
        glBindTextureUnit(1, gbufferNormalTexture.handle());
        glBindTextureUnit(2, gbufferColorTexture.handle());
        quad.draw();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        textRenderer.drawText(font, String.format("FPS: %.2f", framerate), 5f, this.height - 2 * (font.lineHeight() * 0.5f), 0.5f, new Vector3f(1.0f, 0.0f, 0.0f));
        textRenderer.drawText(font, inputSequenceHandler.getDebugSequence(), 5f, this.height - 3 * (font.lineHeight() * 0.5f), 0.5f, new Vector3f(1.0f, 0.0f, 0.0f));
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