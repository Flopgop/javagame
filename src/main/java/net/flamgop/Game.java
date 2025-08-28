package net.flamgop;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiViewport;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.flamgop.asset.AssetKey;
import net.flamgop.asset.AssetLoader;
import net.flamgop.asset.AssetType;
import net.flamgop.gpu.*;
import net.flamgop.gpu.model.Material;
import net.flamgop.level.Level;
import net.flamgop.level.LevelLoader;
import net.flamgop.math.Noise;
import net.flamgop.physics.Physics;
import net.flamgop.screen.PauseScreen;
import net.flamgop.screen.Screen;
import net.flamgop.shadow.ShadowManager;
import net.flamgop.sound.Sound;
import net.flamgop.sound.SoundLoader;
import net.flamgop.sound.SoundManager;
import net.flamgop.sound.SoundSource;
import net.flamgop.text.Font;
import net.flamgop.text.TextRenderer;
import net.flamgop.util.ResourceHelper;
import net.flamgop.util.Util;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import org.renderdoc.api.RenderDoc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Map;

import static org.lwjgl.opengl.GL46.*;

@SuppressWarnings("FieldCanBeLocal")
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

    private final @Nullable RenderDoc renderDoc;

    private final Window window;
    private final ImGuiImplGlfw imGuiGlfw;
    private final ImGuiImplGl3 imGuiGl3;

    private final VertexArray quad;
    private ShaderProgram post;
    private final GPUFramebuffer finalFramebuffer;
    private GPUTexture imgTexture;
    private GPUTexture imgDepthTexture;

    private final ShaderProgram gBufferBlit;
    private final GPUFramebuffer gFramebuffer;
    private GPUTexture gBufferPositionTexture;
    private GPUTexture gBufferNormalTexture;
    private GPUTexture gBufferColorTexture;
    private GPUTexture gBufferMaterialTexture;
    private GPUTexture gBufferDepthTexture;

    private final GPUTexture shadowBlueNoiseTexture;
    private final int shadowResolution = 4096;

    private final TextRenderer textRenderer;
    private final Font font;

    private double frameTime = 1.0f;

    private static final double FRAMERATE_UPDATE_RATE = 1f;
    private double framerate = 1 / frameTime;
    private double framerateUpdateCounter = FRAMERATE_UPDATE_RATE;

    private final int[] passQueries = new int[7];
    private final long[] passTimes = new long[7];

    private final Camera camera;
    private final FrustumCulling frustumCulling;
    private final ClusteredShading clusteredShading;

    private final Physics physics;

    private final Player player;

    private final Level level;

    private final ShadowManager shadowManager;
    private final AssetLoader assetLoader;

    private final SoundManager soundManager;
    private final SoundSource soundSource;
    private final Sound sound;

    private final Screen pauseScreen;
    private @Nullable Screen currentScreen;

    private boolean paused = false;
    private int tonemapMode = 0;

    private final double fixedDeltaTime = 1.0 / 90.0;
    private final int maxSubstepsPerFrame = 20;
    private double accumulator = 0;

    public TextRenderer textRenderer() {
        return textRenderer;
    }

    public Font font() {
        return font;
    }

    public Window window() {
        return window;
    }

    public void screen(@Nullable Screen screen) {
        this.currentScreen = screen;
    }

    public boolean paused() {
        return paused;
    }

    public void paused(boolean paused) {
        if (paused) pause();
        else unpause();
    }

    public Game(@Nullable RenderDoc renderDoc, boolean physicsDebug) throws FileNotFoundException {
        INSTANCE = this;
        this.renderDoc = renderDoc;

        if (renderDoc != null) {
            renderDoc.setCaptureOptionU32(RenderDoc.CaptureOption.API_VALIDATION, 1);
            renderDoc.setCaptureOptionU32(RenderDoc.CaptureOption.CAPTURE_CALLSTACKS, 1);
            renderDoc.setCaptureOptionU32(RenderDoc.CaptureOption.VERIFY_BUFFER_ACCESS, 1);
            renderDoc.setCaptureOptionU32(RenderDoc.CaptureOption.ALLOW_UNSUPPORTED_VENDOR_EXTENSIONS, 1);

            RenderDoc.Version renderDocVersion = renderDoc.getApiVersion();
            System.out.println("RenderDoc " + renderDocVersion.toString() + " loaded.");
        }

        physics = new Physics(4);

        if (!GLFW.glfwInit()) throw new RuntimeException("Failed to initialize GLFW");
        GLFWErrorCallback.createPrint(System.err).set();

        window = new Window("game", 1280, 720, Map.of(
                GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4,
                GLFW.GLFW_CONTEXT_VERSION_MINOR, 6,
                GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE,
                GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE,
                GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE
        ));
        window.setResizeCallback(this::resize);

        window.makeCurrent();

        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        io.setIniFilename(null);

        imGuiGlfw = new ImGuiImplGlfw();
        imGuiGlfw.init(window.handle(), true);

        GL.createCapabilities();

        glEnable(GL_DEBUG_OUTPUT);
        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
        glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, (IntBuffer) null, true);
        glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DEBUG_SEVERITY_NOTIFICATION, (IntBuffer) null, false);
        glDebugMessageCallback((source, type, id, severity, _, message, _) -> {
            System.out.println("OpenGL Debug Message:");
            System.out.println("  Source  : " + getSourceString(source));
            System.out.println("  Type    : " + getTypeString(type));
            System.out.println("  ID      : " + id);
            System.out.println("  Severity: " + getSeverityString(severity));
            System.out.println("  Message : " + MemoryUtil.memUTF8(message));
            System.out.println();
        }, 0L);


        imGuiGl3 = new ImGuiImplGl3();
        imGuiGl3.init("#version 430 core");

        GPUTexture.loadMissingTexture();
        GPUTexture.loadBlit();
        DefaultShaders.loadDefaultShaders();
        Material.loadMissingMaterial();

        this.assetLoader = new AssetLoader("./assets/");

        glCreateQueries(GL_TIME_ELAPSED, passQueries);

        try {
            pauseScreen = new PauseScreen(window, assetLoader);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        post = new ShaderProgram();
        post.attachShaderSource("Post Vertex Shader", ResourceHelper.loadFileContentsFromResource("shaders/post.vertex.glsl"), GL_VERTEX_SHADER);
        post.attachShaderSource("Post Fragment Shader", ResourceHelper.loadFileContentsFromResource("shaders/post.fragment.glsl"), GL_FRAGMENT_SHADER);
        post.link();
        post.label("Post Program");

        gBufferBlit = new ShaderProgram();
        gBufferBlit.attachShaderSource("GBuffer Blit Vertex Shader", ResourceHelper.loadFileContentsFromResource("shaders/gbuffer_blit.vertex.glsl"), GL_VERTEX_SHADER);
        gBufferBlit.attachShaderSource("GBuffer Blit Fragment Shader", ResourceHelper.loadFileContentsFromResource("shaders/gbuffer_blit.fragment.glsl"), GL_FRAGMENT_SHADER);
        gBufferBlit.link();
        gBufferBlit.label("GBuffer Blit Program");

        glProgramUniform1i(gBufferBlit.handle(), gBufferBlit.getUniformLocation("gbuffer_position"), 0);
        glProgramUniform1i(gBufferBlit.handle(), gBufferBlit.getUniformLocation("gbuffer_normal"), 1);
        glProgramUniform1i(gBufferBlit.handle(), gBufferBlit.getUniformLocation("gbuffer_color"), 2);
        glProgramUniform1i(gBufferBlit.handle(), gBufferBlit.getUniformLocation("gbuffer_material"), 3);
        glProgramUniform1i(gBufferBlit.handle(), gBufferBlit.getUniformLocation("gbuffer_depth"), 4);
        glProgramUniform1i(gBufferBlit.handle(), gBufferBlit.getUniformLocation("shadow_blue_noise"), 5);
        glProgramUniform1i(gBufferBlit.handle(), gBufferBlit.getUniformLocation("shadow_depth"), 6);

        glProgramUniform1i(DefaultShaders.GBUFFER.handle(), DefaultShaders.GBUFFER.getUniformLocation("texture_diffuse"), 0);
        glProgramUniform1i(DefaultShaders.GBUFFER.handle(), DefaultShaders.GBUFFER.getUniformLocation("texture_roughness"), 1);
        glProgramUniform1i(DefaultShaders.GBUFFER.handle(), DefaultShaders.GBUFFER.getUniformLocation("texture_metallic"), 2);
        glProgramUniform1i(DefaultShaders.GBUFFER.handle(), DefaultShaders.GBUFFER.getUniformLocation("texture_normal"), 3);

        glProgramUniform1i(post.handle(), post.getUniformLocation("img_texture"), 0);
        glProgramUniform1i(post.handle(), post.getUniformLocation("depth_texture"), 1);
        glProgramUniform1i(post.handle(), post.getUniformLocation("gbuffer_position"), 2);
        glProgramUniform1i(post.handle(), post.getUniformLocation("gbuffer_normal"), 3);
        glProgramUniform1i(post.handle(), post.getUniformLocation("gbuffer_material"), 4);
        glProgramUniform1i(post.handle(), post.getUniformLocation("blue_noise"), 5);

        textRenderer = new TextRenderer(window.width(), window.height());

        quad = new VertexArray();
        quad.data(FRAMEBUFFER_QUAD_VERTICES, 8*Float.BYTES, FRAMEBUFFER_QUAD_INDICES);
        quad.attribute(0, 3, GL_FLOAT, false, 0);
        quad.attribute(1, 3, GL_FLOAT, false, 3 * Float.BYTES);
        quad.attribute(2, 2, GL_FLOAT, false, 6 * Float.BYTES);
        quad.label("GBuffer Quad");

        finalFramebuffer = new GPUFramebuffer(window.width(), window.height(), (fb, w, h) -> {
            imgTexture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
            imgTexture.storage(1, GL_RGBA16F, w, h);
            glTextureParameteri(imgTexture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(imgTexture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            imgTexture.label("Final Color Texture");

            imgDepthTexture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
            imgDepthTexture.storage(1, GL_DEPTH24_STENCIL8, w, h);
            glTextureParameteri(imgDepthTexture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(imgDepthTexture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            imgDepthTexture.label("Final Depth Texture");

            fb.texture(imgTexture, GL_COLOR_ATTACHMENT0, 0);
            fb.texture(imgDepthTexture, GL_DEPTH_STENCIL_ATTACHMENT, 0);

            fb.drawBuffers(new int[]{GL_COLOR_ATTACHMENT0});
        }, (fb) -> {
            imgTexture.destroy();
            imgDepthTexture.destroy();
        });

        gFramebuffer = new GPUFramebuffer(window.width(), window.height(), (fb, w, h) -> {
            gBufferPositionTexture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
            gBufferPositionTexture.storage(1, GL_RGBA16F, w, h);
            glTextureParameteri(gBufferPositionTexture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(gBufferPositionTexture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gBufferPositionTexture.label("GBuffer Position Texture");

            gBufferNormalTexture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
            gBufferNormalTexture.storage(1, GL_RGBA16F, w, h);
            glTextureParameteri(gBufferNormalTexture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(gBufferNormalTexture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gBufferNormalTexture.label("GBuffer Normal Texture");

            gBufferColorTexture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
            gBufferColorTexture.storage(1, GL_RGBA8, w, h);
            glTextureParameteri(gBufferColorTexture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(gBufferColorTexture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gBufferColorTexture.label("GBuffer Color Texture");

            gBufferMaterialTexture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
            gBufferMaterialTexture.storage(1, GL_RGBA16F, w, h);
            glTextureParameteri(gBufferMaterialTexture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(gBufferMaterialTexture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gBufferMaterialTexture.label("GBuffer Material Texture");

            gBufferDepthTexture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
            gBufferDepthTexture.storage(1, GL_DEPTH24_STENCIL8, w, h);
            glTextureParameteri(gBufferDepthTexture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(gBufferDepthTexture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gBufferDepthTexture.label("GBuffer Depth Texture");

            fb.texture(gBufferPositionTexture, GL_COLOR_ATTACHMENT0, 0);
            fb.texture(gBufferNormalTexture, GL_COLOR_ATTACHMENT1, 0);
            fb.texture(gBufferColorTexture, GL_COLOR_ATTACHMENT2, 0);
            fb.texture(gBufferMaterialTexture, GL_COLOR_ATTACHMENT3, 0);
            fb.texture(gBufferDepthTexture, GL_DEPTH_STENCIL_ATTACHMENT, 0);

            fb.drawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3});
        }, (fb) -> {
            gBufferPositionTexture.destroy();
            gBufferNormalTexture.destroy();
            gBufferColorTexture.destroy();
            gBufferMaterialTexture.destroy();
            gBufferDepthTexture.destroy();
        });
        gFramebuffer.label("GBuffer");

        try {
            shadowBlueNoiseTexture = GPUTexture.loadFromBytes(assetLoader.load(new AssetKey(AssetType.RESOURCE, "bluenoise.png")));
            glTextureParameteri(shadowBlueNoiseTexture.handle(), GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTextureParameteri(shadowBlueNoiseTexture.handle(), GL_TEXTURE_WRAP_T, GL_REPEAT);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        LevelLoader loader = new LevelLoader(assetLoader);
        this.level = loader.load(physics, ResourceHelper.loadFileContentsFromResource("example_level.json5"));

        if (physicsDebug) {
            this.level.scene().setupDebug();
        }

        camera = new Camera(
                new Vector3f(4.0f, 20.0f, 4.0f),
                new Vector3f(0.0f),
                new Vector3f(0f, 1f, 0f),
                (float) Math.toRadians(60f),
                (float) window.width() / window.height(),
                0.01f,
                100f
        );
        clusteredShading = new ClusteredShading(level);
        shadowManager = new ShadowManager(camera, shadowResolution, 4);
        frustumCulling = new FrustumCulling(shadowManager, level.skylight(), shadowResolution, camera);

        font = new Font(ResourceHelper.loadFileFromResource("Nunito.ttf"), 512, 1, 1024, 1024);

        font.writeAtlasToDisk(new File("font.png"));

        this.player = new Player(physics, level.scene(), camera, window.inputState());
        this.soundManager = new SoundManager(player);
        this.soundSource = new SoundSource();
        this.sound = SoundLoader.loadWav(assetLoader.load(new AssetKey(AssetType.RESOURCE, "sound.wav")));
        if (!sound.valid()) throw new RuntimeException("Sound isn't valid!");
    }

    private void start() {
        window.setCursorMode(GLFW.GLFW_CURSOR_DISABLED);
        window.show();
        window.focusWindow();
        window.requestAttention();
        this.soundSource.playSound(sound);

        while (!this.shouldClose()) {
            double time = GLFW.glfwGetTime();
            this.update(frameTime);
            this.render(frameTime);
            this.swap();
            frameTime = GLFW.glfwGetTime() - time;
        }

        this.cleanup();
    }

    public Level level() {
        return level;
    }

    public FrustumCulling culling() {
        return this.frustumCulling;
    }

    public void unpause() {
        paused = false;
        window.setCursorMode(GLFW.GLFW_CURSOR_DISABLED);
        this.currentScreen = null;
    }
    public void pause() {
        paused = true;
        window.setCursorMode(GLFW.GLFW_CURSOR_NORMAL);
        this.currentScreen = pauseScreen;
    }

    private void fixedUpdate(double delta) {
        accumulator += delta;

        int steps = 0;
        while (accumulator >= fixedDeltaTime && steps < maxSubstepsPerFrame) {
            level.scene().fixedUpdate((float) fixedDeltaTime);
            player.fixedUpdate(fixedDeltaTime);

            accumulator -= fixedDeltaTime;
            steps++;
        }

        if (steps >= maxSubstepsPerFrame && accumulator >= fixedDeltaTime) {
            System.err.printf("Physics running ~%.2f steps behind!\n", accumulator / fixedDeltaTime);
            accumulator = 0.0;
        }
    }

    private boolean shouldClose() {
        return this.window.shouldClose();
    }

    private void resize(int width, int height) {
        this.camera.resize(width, height);
        this.gFramebuffer.resize(width, height);
        this.finalFramebuffer.resize(width, height);
        this.textRenderer.resize(width, height);
    }

    private boolean overlayEnabled = true;
    private void update(double delta) {
        GLFW.glfwPollEvents();

        framerateUpdateCounter -= delta;
        if (framerateUpdateCounter < 0) {
            framerateUpdateCounter = FRAMERATE_UPDATE_RATE;
            framerate = 1 / delta;
        }

        if (window.inputState().wasKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
            if (paused) unpause();
            else pause();
        }

        if (window.inputState().wasKeyPressed(GLFW.GLFW_KEY_V)) {
            tonemapMode++;
            if (tonemapMode > 3) tonemapMode = 0;
        }

        if (renderDoc != null && window.inputState().wasKeyPressed(GLFW.GLFW_KEY_PERIOD)) {
            if (overlayEnabled) {
                overlayEnabled = false;
                renderDoc.maskOverlayBits(RenderDoc.OverlayBits.NONE.bits(), RenderDoc.OverlayBits.NONE.bits());
            } else {
                overlayEnabled = true;
                renderDoc.maskOverlayBits(RenderDoc.OverlayBits.DEFAULT.bits(), RenderDoc.OverlayBits.DEFAULT.bits());
            }
        }

        if (window.inputState().wasKeyPressed(GLFW.GLFW_KEY_P)) {
            post.destroy();
            try {
                post = new ShaderProgram();
                post.attachShaderSource("Post Vertex Shader", assetLoader.loadAsString(new AssetKey(AssetType.FILE, "shaders/post.vertex.glsl")), GL_VERTEX_SHADER);
                post.attachShaderSource("Post Fragment Shader", assetLoader.loadAsString(new AssetKey(AssetType.FILE, "shaders/post.fragment.glsl")), GL_FRAGMENT_SHADER);
                post.link();
                post.label("Post Program");
            } catch (FileNotFoundException e) {
                post.destroy();
                try {
                    System.out.println("WARNING: failed to reload post shader from assets (doesn't exist?). Using fallback.");
                    post = new ShaderProgram();
                    post.attachShaderSource("Post Vertex Shader", assetLoader. loadAsString(new AssetKey(AssetType.RESOURCE, "shaders/post.vertex.glsl")), GL_VERTEX_SHADER);
                    post.attachShaderSource("Post Fragment Shader", assetLoader.loadAsString(new AssetKey(AssetType.RESOURCE, "shaders/post.fragment.glsl")), GL_FRAGMENT_SHADER);
                    post.link();
                    post.label("Post Program");
                } catch (FileNotFoundException ne) {
                    throw new RuntimeException(ne);
                }
            }
            glProgramUniform1i(post.handle(), post.getUniformLocation("img_texture"), 0);
            glProgramUniform1i(post.handle(), post.getUniformLocation("depth_texture"), 1);
            glProgramUniform1i(post.handle(), post.getUniformLocation("gbuffer_position"), 2);
            glProgramUniform1i(post.handle(), post.getUniformLocation("gbuffer_normal"), 3);
            glProgramUniform1i(post.handle(), post.getUniformLocation("gbuffer_material"), 4);
            glProgramUniform1i(post.handle(), post.getUniformLocation("blue_noise"), 5);
        }

        if (!paused) {
            fixedUpdate(delta);
            level.update(delta);
            player.update(delta);
            this.soundManager.update();
        }

        if (currentScreen != null) {
            currentScreen.update(delta);
        }
        window.update();

        this.culling().update();

        for (int i = 0; i < passQueries.length; i++) {
            if (Util.isQueryReady(passQueries[i])) {
                this.passTimes[i] = Util.getQueryTime(passQueries[i]);
            }
        }
    }

    private void renderShadowPass(double delta) {
        glPushDebugGroup(GL_DEBUG_SOURCE_APPLICATION, 1, "Shadow Pass");
        glBeginQuery(GL_TIME_ELAPSED, passQueries[0]);

        this.shadowManager.bindFramebuffer();

        glFrontFace(GL_CCW);
        glCullFace(GL_BACK);

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        culling().shadow(true);
        shadowManager.prepareShadowPass();
        shadowManager.bindUniforms(null, ShadowManager.getShadowView(this.level.skylight()));
        level.render(delta);
        shadowManager.finishShadowPass();
        culling().shadow(false);

        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);

        glEndQuery(GL_TIME_ELAPSED);
        glPopDebugGroup();
    }

    private void renderGBufferPass(double delta) {
        glPushDebugGroup(GL_DEBUG_SOURCE_APPLICATION, 3, "GBuffer Pass");
        glBeginQuery(GL_TIME_ELAPSED, passQueries[1]);

        glViewport(0,0,window.width(),window.height());
        glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        gFramebuffer.clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glFrontFace(GL_CCW);
        glCullFace(GL_BACK);

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        camera.bind(0);
        level.render(delta);

        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);

        glEndQuery(GL_TIME_ELAPSED);
        glPopDebugGroup();
    }

    private void renderGBufferBlit(double delta) {
        glPushDebugGroup(GL_DEBUG_SOURCE_APPLICATION, 4, "GBuffer Blit");
        glBeginQuery(GL_TIME_ELAPSED, passQueries[2]);

        gFramebuffer.unbind();

        finalFramebuffer.clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        camera.bind(0);
        level.pbrUniformBuffer().bind(1);
        level.lightSSBO().bind(2);
        clusteredShading.clusterGridSSBO().bind(3);
        gBufferBlit.use();
        glBindTextureUnit(0, gBufferPositionTexture.handle());
        glBindTextureUnit(1, gBufferNormalTexture.handle());
        glBindTextureUnit(2, gBufferColorTexture.handle());
        glBindTextureUnit(3, gBufferMaterialTexture.handle());
        glBindTextureUnit(4, gBufferDepthTexture.handle());
        glBindTextureUnit(5, shadowBlueNoiseTexture.handle());
        glBindTextureUnit(6, this.shadowManager.texture().handle());
        glProgramUniform1f(gBufferBlit.handle(), gBufferBlit.getUniformLocation("z_near"), camera.near());
        glProgramUniform1f(gBufferBlit.handle(), gBufferBlit.getUniformLocation("z_far"), camera.far());
        glProgramUniform3i(gBufferBlit.handle(), gBufferBlit.getUniformLocation("grid_size"), ClusteredShading.GRID_SIZE_X, ClusteredShading.GRID_SIZE_Y, ClusteredShading.GRID_SIZE_Z);
        glProgramUniform2i(gBufferBlit.handle(), gBufferBlit.getUniformLocation("screen_dimensions"), window.width(), window.height());
        shadowManager.bindUniforms(gBufferBlit, ShadowManager.getShadowView(this.level.skylight()));
        quad.draw();
        gFramebuffer.copyDepthToBuffer(finalFramebuffer, this.window.width(), this.window.height());

        glEndQuery(GL_TIME_ELAPSED);
        glPopDebugGroup();
    }

    private void renderForwardPass(double delta) {
        glPushDebugGroup(GL_DEBUG_SOURCE_APPLICATION, 6, "Forward Pass");
        glBeginQuery(GL_TIME_ELAPSED, passQueries[3]);

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        // render

        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);

        glEndQuery(GL_TIME_ELAPSED);
        glPopDebugGroup();
    }

    private void renderPost(double delta) {
        glPushDebugGroup(GL_DEBUG_SOURCE_APPLICATION, 5, "Post");
        glBeginQuery(GL_TIME_ELAPSED, passQueries[4]);

        gFramebuffer.unbind();

        camera.bind(0);
        post.use();
        glBindTextureUnit(0, imgTexture.handle());
        glBindTextureUnit(1, imgDepthTexture.handle());
        glBindTextureUnit(2, gBufferPositionTexture.handle());
        glBindTextureUnit(3, gBufferNormalTexture.handle());
        glBindTextureUnit(4, gBufferMaterialTexture.handle());
        glBindTextureUnit(5, shadowBlueNoiseTexture.handle());
        glProgramUniform1i(post.handle(), post.getUniformLocation("tonemap_mode"), tonemapMode);
        glProgramUniform2i(post.handle(), post.getUniformLocation("screen_size"), this.window.width(), this.window.height());
        glProgramUniform1f(post.handle(), post.getUniformLocation("z_near"), this.camera.near());
        glProgramUniform1f(post.handle(), post.getUniformLocation("z_far"), this.camera.far());
        quad.draw();
        finalFramebuffer.copyDepthToBackBuffer(this.window.width(), this.window.height());

        glEndQuery(GL_TIME_ELAPSED);
        glPopDebugGroup();
    }

    private String intToPassName(int i) {
        return switch (i) {
            case 0 -> "Shadow";
            case 1 -> "GBuffer";
            case 2 -> "GBuffer Blit";
            case 3 -> "Forward";
            case 4 -> "Post";
            case 5 -> "UI";
            case 6 -> "ImGui";
            default -> ""+i;
        };
    }

    float[] gain = new float[]{1.0f};
    private void renderImGui() {
        glPushDebugGroup(GL_DEBUG_SOURCE_APPLICATION, 7, "ImGui Pass");
        glBeginQuery(GL_TIME_ELAPSED, passQueries[6]);
        imGuiGl3.newFrame();
        imGuiGlfw.newFrame();
        ImGui.newFrame();

        ImGuiViewport viewport = ImGui.getMainViewport();

        int flags = ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus | ImGuiWindowFlags.NoBackground;

        ImGui.setNextWindowPos(viewport.getPos());
        ImGui.setNextWindowSize(viewport.getSize());
        ImGui.setNextWindowViewport(viewport.getID());

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        ImGui.begin("dockspace", null, flags);
        ImGui.popStyleVar(2);

        int id = ImGui.getID("mydockspace");
        ImGui.dockSpace(id, new ImVec2(0, 0), ImGuiDockNodeFlags.PassthruCentralNode);

        ImGui.end();

        if (ImGui.begin("Settings", ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.sliderFloat("Gain", gain, 0, 1);
            if (ImGui.isItemDeactivatedAfterEdit()) {
                this.soundManager.masterGain(gain[0]);
            }
        }
        ImGui.end();

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            if (overlayEnabled && renderDoc != null) renderDoc.maskOverlayBits(RenderDoc.OverlayBits.NONE.bits(), RenderDoc.OverlayBits.NONE.bits());
            final long backupCurrentContext = GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            GLFW.glfwMakeContextCurrent(backupCurrentContext);
            if (overlayEnabled && renderDoc != null) renderDoc.maskOverlayBits(RenderDoc.OverlayBits.DEFAULT.bits(), RenderDoc.OverlayBits.DEFAULT.bits());
        }

        glEndQuery(GL_TIME_ELAPSED);
        glPopDebugGroup();
    }

    private void renderUi(double delta) {
        glPushDebugGroup(GL_DEBUG_SOURCE_APPLICATION, 6, "UI Pass");
        glBeginQuery(GL_TIME_ELAPSED, passQueries[5]);

        glEnable(GL_DEPTH_TEST);
        this.level.scene().renderDebug(this.camera); // this is done here because it renders into the backbuffer, bypassing post-processing.
        glDisable(GL_DEPTH_TEST);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        float textScale = 1f / 3f;
        textRenderer.drawText(font, String.format("FPS: %.2f", framerate), 5f, this.window.height() - 3 * (font.lineHeight() * textScale), textScale, new Vector3f(1.0f, 0.0f, 0.0f));
        textRenderer.drawText(font, this.window.inputSequenceHandler().getDebugSequence(), 5f, this.window.height() - 4 * (font.lineHeight() * textScale), textScale, new Vector3f(1.0f, 0.0f, 0.0f));
        player.renderDebug(textRenderer, 5f, this.window.height() - 5 * (this.font.lineHeight() * textScale), textScale, delta);

        for (int i = 0; i < passQueries.length; i++) {
            textRenderer.drawText(font, String.format("Pass %s took %.3fms", intToPassName(i), ((float)this.passTimes[i] / 1e6)), 5f, this.window.height() - (8 + i) * (this.font.lineHeight() * textScale), textScale, new Vector3f(1.0f, 0.0f, 0.0f));
        }

        textRenderer.drawText(font, String.format("Clustered gathering took %.3fms", ((float)clusteredShading.gatherTimeNs() / 1e6)), 5f, this.window.height() - (8 + passQueries.length) * (this.font.lineHeight() * textScale), textScale, new Vector3f(1.0f, 0.0f, 0.0f));
        textRenderer.drawText(font, String.format("Clustered culling took %.3fms", ((float)clusteredShading.cullTimeNs() / 1e6)), 5f, this.window.height() - (9 + passQueries.length) * (this.font.lineHeight() * textScale), textScale, new Vector3f(1.0f, 0.0f, 0.0f));

        if (currentScreen != null) {
            currentScreen.render(delta);
        }

        glDisable(GL_BLEND);
        glEndQuery(GL_TIME_ELAPSED);
        glPopDebugGroup();
    }

    private void render(double delta) {
        glPushDebugGroup(GL_DEBUG_SOURCE_APPLICATION, 0, "Frame");
        glClearColor(1.0f, 0.0f, 0.0f, 1.0f); // backbuffer color is bright fucking red so that we know when the framebuffer isn't drawing properly
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        renderShadowPass(delta);

        clusteredShading.compute(camera);

        // draw to framebuffer
        renderGBufferPass(delta);

        renderGBufferBlit(delta);

        renderForwardPass(delta);

        renderPost(delta);

        renderUi(delta);

        renderImGui();

        glPopDebugGroup();
    }

    private void swap() {
        window.swap();
    }

    private void cleanup() {
        this.window.destroy();
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) throws IOException {
        boolean renderdocEnabled = false;
        boolean physicsDebug = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-renderdoc")) renderdocEnabled = true;
            else if (arg.equalsIgnoreCase("-physics-debug")) physicsDebug = true;
        }
        Game game = new Game(renderdocEnabled ? RenderDoc.load(RenderDoc.KnownVersion.API_1_6_0) : null, physicsDebug);
        game.start();
    }
}