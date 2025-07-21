package net.flamgop;

import net.flamgop.asset.AssetLoader;
import net.flamgop.gpu.*;
import net.flamgop.gpu.buffer.GPUBuffer;
import net.flamgop.gpu.buffer.ShaderStorageBuffer;
import net.flamgop.gpu.buffer.UniformBuffer;
import net.flamgop.gpu.model.Material;
import net.flamgop.input.InputSequenceHandler;
import net.flamgop.input.InputState;
import net.flamgop.level.Level;
import net.flamgop.level.LevelLoader;
import net.flamgop.physics.Physics;
import net.flamgop.text.Font;
import net.flamgop.text.TextRenderer;
import net.flamgop.gpu.uniform.PBRUniformData;
import net.flamgop.util.ColorUtil;
import net.flamgop.util.ResourceHelper;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL46.*;

public class Game {

    public static Game INSTANCE;

    private static String getSourceString(int source) {
        return switch (source) {
            case GL_DEBUG_SOURCE_API -> "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "Window System";
            case GL_DEBUG_SOURCE_SHADER_COMPILER -> "Shader Compiler";
            case GL_DEBUG_SOURCE_THIRD_PARTY -> "Third Party";
            case GL_DEBUG_SOURCE_APPLICATION -> "Application";
            case GL_DEBUG_SOURCE_OTHER -> "Other";
            default -> "Unknown";
        };
    }

    private static String getTypeString(int type) {
        return switch (type) {
            case GL_DEBUG_TYPE_ERROR -> "Error";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "Deprecated Behavior";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "Undefined Behavior";
            case GL_DEBUG_TYPE_PORTABILITY -> "Portability";
            case GL_DEBUG_TYPE_PERFORMANCE -> "Performance";
            case GL_DEBUG_TYPE_MARKER -> "Marker";
            case GL_DEBUG_TYPE_PUSH_GROUP -> "Push Group";
            case GL_DEBUG_TYPE_POP_GROUP -> "Pop Group";
            case GL_DEBUG_TYPE_OTHER -> "Other";
            default -> "Unknown";
        };
    }

    private static String getSeverityString(int severity) {
        return switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH -> "High";
            case GL_DEBUG_SEVERITY_MEDIUM -> "Medium";
            case GL_DEBUG_SEVERITY_LOW -> "Low";
            case GL_DEBUG_SEVERITY_NOTIFICATION -> "Notification";
            default -> "Unknown";
        };
    }

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

    private final VertexArray quad;
    private final GPUFramebuffer framebuffer;

    private final ShaderProgram gBufferBlit;
    private GPUTexture gBufferPositionTexture;
    private GPUTexture gBufferNormalTexture;
    private GPUTexture gBufferColorTexture;

    private final ShaderStorageBuffer lightSSBO;
    private final UniformBuffer pbrUniformBuffer;

    private final TextRenderer textRenderer;
    private final Font font;

    private double frameTime = 1.0f;

    private static final double FRAMERATE_UPDATE_RATE = 1f;
    private double framerate = 1 / frameTime;
    private double framerateUpdateCounter = FRAMERATE_UPDATE_RATE;

    private final Camera camera;
    private final InputSequenceHandler inputSequenceHandler;
    private final InputState inputState;

    @SuppressWarnings("FieldCanBeLocal")
    private final Physics physics;

//    private final VertexArray cubeMesh;

    private final Player player;

    private final Level level;

    private int width, height;

    public TextRenderer textRenderer() {
        return textRenderer;
    }

    public Font font() {
        return font;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public Game() {
        INSTANCE = this;
        physics = new Physics(4);

        if (!GLFW.glfwInit()) throw new RuntimeException("Failed to initialize GLFW");

        this.width = 1280;
        this.height = 720;

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
        this.window = GLFW.glfwCreateWindow(width, height, "game", 0L, 0L);
        GLFWErrorCallback.createPrint(System.err).set();

        this.inputSequenceHandler = new InputSequenceHandler();
        this.inputState = new InputState();

        //noinspection resource
        GLFW.glfwSetKeyCallback(window, (_, key, _, action, mods) -> {
            inputSequenceHandler.handleKey(key, mods, action);
            inputState.handleKey(key, action);
        });
        //noinspection resource
        GLFW.glfwSetCursorPosCallback(window, (_, x, y) -> {
            inputState.handleMouse(x,y);
        });
        //noinspection resource
        GLFW.glfwSetMouseButtonCallback(window, (_, button, action, _) -> {
            inputState.handleMouseButton(button, action);
        });

        GLFW.glfwMakeContextCurrent(this.window);
        GL.createCapabilities();

        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
        glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, (IntBuffer) null, true);
        glDebugMessageCallback((source, type, id, severity, _, message, _) -> {
            System.out.println("OpenGL Debug Message:");
            System.out.println("  Source  : " + getSourceString(source));
            System.out.println("  Type    : " + getTypeString(type));
            System.out.println("  ID      : " + id);
            System.out.println("  Severity: " + getSeverityString(severity));
            System.out.println("  Message : " + message);
            System.out.println();
        }, 0L);

        GPUTexture.loadMissingTexture();
        DefaultShaders.loadDefaultShaders();
        Material.loadMissingMaterial();

        AssetLoader assetLoader = new AssetLoader("./assets/");

        LevelLoader loader = new LevelLoader(assetLoader);
        this.level = loader.load(physics, ResourceHelper.loadFileContentsFromResource("example_level.json5"));

        gBufferBlit = new ShaderProgram();
        gBufferBlit.attachShaderSource(ResourceHelper.loadFileContentsFromResource("gbuffer_blit.vertex.glsl"), GL_VERTEX_SHADER);
        gBufferBlit.attachShaderSource(ResourceHelper.loadFileContentsFromResource("gbuffer_blit.fragment.glsl"), GL_FRAGMENT_SHADER);
        gBufferBlit.link();

        glProgramUniform1i(gBufferBlit.handle(), gBufferBlit.getUniformLocation("gbuffer_position"), 0);
        glProgramUniform1i(gBufferBlit.handle(), gBufferBlit.getUniformLocation("gbuffer_normal"), 1);
        glProgramUniform1i(gBufferBlit.handle(), gBufferBlit.getUniformLocation("gbuffer_color"), 2);
        
        textRenderer = new TextRenderer(width, height);

        quad = new VertexArray();
        quad.data(FRAMEBUFFER_QUAD_VERTICES, 8*Float.BYTES, FRAMEBUFFER_QUAD_INDICES);
        quad.attribute(0, 3, GL_FLOAT, false, 0);
        quad.attribute(1, 3, GL_FLOAT, false, 3 * Float.BYTES);
        quad.attribute(2, 2, GL_FLOAT, false, 6 * Float.BYTES);

        framebuffer = new GPUFramebuffer(width, height, (fb, w, h) -> {
            gBufferPositionTexture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
            gBufferPositionTexture.storage(1, GL_RGBA16F, w, h);
            glTextureParameteri(gBufferPositionTexture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(gBufferPositionTexture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            gBufferNormalTexture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
            gBufferNormalTexture.storage(1, GL_RGBA16F, w, h);
            glTextureParameteri(gBufferNormalTexture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(gBufferNormalTexture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            gBufferColorTexture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
            gBufferColorTexture.storage(1, GL_RGBA8, w, h);
            glTextureParameteri(gBufferColorTexture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(gBufferColorTexture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            fb.texture(gBufferPositionTexture, GL_COLOR_ATTACHMENT0, 0);
            fb.texture(gBufferNormalTexture, GL_COLOR_ATTACHMENT1, 0);
            fb.texture(gBufferColorTexture, GL_COLOR_ATTACHMENT2, 0);

            fb.renderbuffer(GL_DEPTH24_STENCIL8, GL_DEPTH_STENCIL_ATTACHMENT, w, h);

            fb.drawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2});
        });

        pbrUniformBuffer = new UniformBuffer(GPUBuffer.UpdateHint.STATIC);

        PBRUniformData pbr = new PBRUniformData();
        pbr.ambient = new Vector4f(ColorUtil.getRGBFromK(5900), 0.3f);
        pbr.lightColor = new Vector4f(ColorUtil.getRGBFromK(5900), 5f);
        pbr.lightCount = 2;
        pbr.lightDirection = new Vector3f(0f, 0.829f, -0.559f);
        pbrUniformBuffer.allocate(pbr);

        lightSSBO = new ShaderStorageBuffer(GPUBuffer.UpdateHint.DYNAMIC);
        lightSSBO.allocate(level.lights());

        camera = new Camera(
                new Vector3f(4.0f, 20.0f, 4.0f),
                new Vector3f(0.0f),
                new Vector3f(0f, 1f, 0f),
                (float) Math.toRadians(60f),
                (float) width / height,
                0.01f,
                1000f
        );

//        modelTexture = GPUTexture.loadFromBytes(ResourceHelper.loadFileFromResource("cocount.jpg"));

        font = new Font(ResourceHelper.loadFileFromResource("Nunito.ttf"), 512, 1, 1024, 1024);

        font.writeAtlasToDisk(new File("font.png"));

        this.player = new Player(physics, level.scene(), camera, inputState);
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
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

        level.update(player, delta);
        player.update(delta);

        inputState.update();
    }

    private void renderGBufferPass(double delta) {
        camera.bind(0);
        level.render(delta);
    }

    private void renderForwardPass(double delta) {

    }

    private void renderUi(double delta) {
        textRenderer.drawText(font, String.format("FPS: %.2f", framerate), 5f, this.height - 2 * (font.lineHeight() * 0.5f), 0.5f, new Vector3f(1.0f, 0.0f, 0.0f));
        textRenderer.drawText(font, inputSequenceHandler.getDebugSequence(), 5f, this.height - 3 * (font.lineHeight() * 0.5f), 0.5f, new Vector3f(1.0f, 0.0f, 0.0f));
        player.renderDebug(textRenderer, delta);
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
        renderGBufferPass(delta);

        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);

        framebuffer.unbind();
        camera.bind(0);
        pbrUniformBuffer.bind(1);
        lightSSBO.bind(2);
        gBufferBlit.use();
        glBindTextureUnit(0, gBufferPositionTexture.handle());
        glBindTextureUnit(1, gBufferNormalTexture.handle());
        glBindTextureUnit(2, gBufferColorTexture.handle());
        quad.draw();
        framebuffer.copyDepthToBackBuffer(width, height);

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        renderForwardPass(delta);

        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        renderUi(delta);
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