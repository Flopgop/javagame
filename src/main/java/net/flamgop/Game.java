package net.flamgop;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiViewport;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.flamgop.asset.*;
import net.flamgop.asset.loaders.*;
import net.flamgop.entity.Entity;
import net.flamgop.entity.Scene;
import net.flamgop.entity.components.*;
import net.flamgop.gpu.*;
import net.flamgop.gpu.debug.DebugLogging;
import net.flamgop.gpu.framebuffer.GPUFramebuffer;
import net.flamgop.gpu.model.Material;
import net.flamgop.gpu.model.Model;
import net.flamgop.gpu.state.*;
import net.flamgop.gpu.texture.GPUTexture;
import net.flamgop.gpu.texture.TextureFormat;
import net.flamgop.gpu.vertex.Attribute;
import net.flamgop.gpu.vertex.VertexArray;
import net.flamgop.gpu.vertex.VertexFormat;
import net.flamgop.physics.Physics;
import net.flamgop.physics.PhysicsShape;
import net.flamgop.screen.PauseScreen;
import net.flamgop.screen.Screen;
import net.flamgop.shadow.ShadowManager;
import net.flamgop.sound.Sound;
import net.flamgop.sound.SoundManager;
import net.flamgop.sound.SoundSource;
import net.flamgop.text.Font;
import net.flamgop.text.TextRenderer;
import net.flamgop.util.LazyInit;
import net.flamgop.util.ResourceHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.renderdoc.api.RenderDoc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("FieldCanBeLocal")
public class Game {

    public static Game INSTANCE;

    private static final LazyInit<VertexFormat> FRAMEBUFFER_VERTEX_FORMAT = new LazyInit<>(() -> VertexFormat.builder()
            .attribute(0, Attribute.of(Attribute.Type.FLOAT, 3, false))
            .attribute(1, Attribute.of(Attribute.Type.FLOAT, 3, false))
            .attribute(2, Attribute.of(Attribute.Type.FLOAT, 2, false))
            .build());

    private static final float[] FRAMEBUFFER_QUAD_VERTICES = new float[]{
            -1, 0, -1,  0, 1, 0,  0, 0, // vertex position, normal, uv
             1, 0, -1,  0, 1, 0,  1, 0,
             1, 0,  1,  0, 1, 0,  1, 1,
            -1, 0,  1,  0, 1, 0,  0, 1
    };
    private static final byte[] FRAMEBUFFER_QUAD_INDICES = new byte[]{
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

    private final ShadowManager shadowManager;
    private final GPUTexture shadowBlueNoiseTexture;
    private final int shadowResolution = 4096;

    private final TextRenderer textRenderer;
    private final Font font;

    private double frameTime = 1.0f;

    private static final double FRAMERATE_UPDATE_RATE = 1f;
    private double framerate = 1 / frameTime;
    private double framerateUpdateCounter = FRAMERATE_UPDATE_RATE;

    private final Query[] passQueries = new Query[7];
    private final long[] passTimes = new long[7];

    private final Camera camera;
    private final FrustumCulling frustumCulling;
    private final ClusteredShading clusteredShading;

    private final Physics physics;

    private final Player player;

    private final Scene scene;
    private final Entity god;

    private final AssetManager assetManager;

    private final SoundManager soundManager;
    private final SoundSource soundSource;
    private final Sound sound;

    private final Screen pauseScreen;
    private @Nullable Screen currentScreen;

    private boolean paused = false;
    private int tonemapMode = 2;

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

    public Physics physics() {
        return physics;
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

    public Game(@Nullable RenderDoc renderDoc, boolean physicsDebug) {
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

        try {
            physics = new Physics();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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

        DebugLogging.enable();

        imGuiGl3 = new ImGuiImplGl3();
        imGuiGl3.init("#version 430 core");

        this.assetManager = new AssetManager();
        this.assetManager.registerLoader(Model.class, new ModelLoader(assetManager));
        this.assetManager.registerLoader(ByteBuffer.class, new RawLoader());
        this.assetManager.registerLoader(GPUTexture.class, new TextureLoader());
        this.assetManager.registerLoader(String.class, new StringLoader());
        this.assetManager.registerLoader(Sound.class, new SoundLoader());
        this.assetManager.registerLoader(PhysicsShape.class, new PhysicsShapeLoader());
        this.assetManager.registerLoader(Scene.class, new SceneLoader(assetManager, physics));

        GPUTexture.loadMissingTexture(assetManager);
        GPUTexture.loadBlit();
        DefaultShaders.loadDefaultShaders();
        Material.loadMissingMaterial();

        for (int i = 0; i < passQueries.length; i++) {
            passQueries[i] = new Query(Query.QueryTarget.TIME_ELAPSED);
            passQueries[i].label(intToPassName(i) + " Time Elapsed Query");
        }

        try {
            pauseScreen = new PauseScreen(window, assetManager);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        post = new ShaderProgram();
        post.attachShaderSource("Post Vertex Shader", ResourceHelper.loadFileContentsFromResource("shaders/post.vertex.glsl"), ShaderProgram.ShaderType.VERTEX);
        post.attachShaderSource("Post Fragment Shader", ResourceHelper.loadFileContentsFromResource("shaders/post.fragment.glsl"), ShaderProgram.ShaderType.FRAGMENT);
        post.link();
        post.label("Post Program");

        gBufferBlit = new ShaderProgram();
        gBufferBlit.attachShaderSource("GBuffer Blit Vertex Shader", ResourceHelper.loadFileContentsFromResource("shaders/gbuffer_blit.vertex.glsl"), ShaderProgram.ShaderType.VERTEX);
        gBufferBlit.attachShaderSource("GBuffer Blit Fragment Shader", ResourceHelper.loadFileContentsFromResource("shaders/gbuffer_blit.fragment.glsl"), ShaderProgram.ShaderType.FRAGMENT);
        gBufferBlit.link();
        gBufferBlit.label("GBuffer Blit Program");

        gBufferBlit.uniform1i(gBufferBlit.getUniformLocation("gbuffer_position"), 0);
        gBufferBlit.uniform1i(gBufferBlit.getUniformLocation("gbuffer_normal"), 1);
        gBufferBlit.uniform1i(gBufferBlit.getUniformLocation("gbuffer_color"), 2);
        gBufferBlit.uniform1i(gBufferBlit.getUniformLocation("gbuffer_material"), 3);
        gBufferBlit.uniform1i(gBufferBlit.getUniformLocation("gbuffer_depth"), 4);
        gBufferBlit.uniform1i(gBufferBlit.getUniformLocation("shadow_blue_noise"), 5);
        gBufferBlit.uniform1i(gBufferBlit.getUniformLocation("shadow_depth"), 6);

        DefaultShaders.GBUFFER.uniform1i(DefaultShaders.GBUFFER.getUniformLocation("texture_diffuse"), 0);
        DefaultShaders.GBUFFER.uniform1i(DefaultShaders.GBUFFER.getUniformLocation("texture_roughness"), 1);
        DefaultShaders.GBUFFER.uniform1i(DefaultShaders.GBUFFER.getUniformLocation("texture_metallic"), 2);
        DefaultShaders.GBUFFER.uniform1i(DefaultShaders.GBUFFER.getUniformLocation("texture_normal"), 3);

        post.uniform1i(post.getUniformLocation("img_texture"), 0);
        post.uniform1i(post.getUniformLocation("depth_texture"), 1);
        post.uniform1i(post.getUniformLocation("gbuffer_position"), 2);
        post.uniform1i(post.getUniformLocation("gbuffer_normal"), 3);
        post.uniform1i(post.getUniformLocation("gbuffer_material"), 4);
        post.uniform1i(post.getUniformLocation("blue_noise"), 5);

        textRenderer = new TextRenderer(window.width(), window.height());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer vertices = MemoryUtil.memByteBuffer(stack.floats(FRAMEBUFFER_QUAD_VERTICES));
            ByteBuffer indices = stack.bytes(FRAMEBUFFER_QUAD_INDICES);

            quad = new VertexArray(FRAMEBUFFER_VERTEX_FORMAT.get());
            quad.data(vertices, 0, 0);
            quad.elementData(indices, VertexArray.IndexType.UNSIGNED_BYTE, FRAMEBUFFER_QUAD_INDICES.length);

            quad.label("GBuffer Quad");
        }

        finalFramebuffer = new GPUFramebuffer(window.width(), window.height(), (fb, w, h) -> {
            imgTexture = new GPUTexture(GPUTexture.Target.TEXTURE_2D);
            imgTexture.storage(1, TextureFormat.RGBA16F, w, h);
            imgTexture.minFilter(GPUTexture.MinFilter.NEAREST);
            imgTexture.magFilter(GPUTexture.MagFilter.NEAREST);
            imgTexture.label("Final Color Texture");

            imgDepthTexture = new GPUTexture(GPUTexture.Target.TEXTURE_2D);
            imgDepthTexture.storage(1, TextureFormat.DEPTH24_STENCIL8, w, h);
            imgDepthTexture.minFilter(GPUTexture.MinFilter.NEAREST);
            imgDepthTexture.magFilter(GPUTexture.MagFilter.NEAREST);
            imgDepthTexture.label("Final Depth Texture");

            fb.texture(imgTexture, GPUFramebuffer.Attachment.COLOR0, 0);
            fb.texture(imgDepthTexture, GPUFramebuffer.Attachment.DEPTH_STENCIL, 0);

            fb.drawBuffers(new GPUFramebuffer.Attachment[]{GPUFramebuffer.Attachment.COLOR0});
        }, (fb) -> {
            imgTexture.destroy();
            imgDepthTexture.destroy();
        });
        finalFramebuffer.label("Final Framebuffer");

        gFramebuffer = new GPUFramebuffer(window.width(), window.height(), (fb, w, h) -> {
            gBufferPositionTexture = new GPUTexture(GPUTexture.Target.TEXTURE_2D);
            gBufferPositionTexture.storage(1, TextureFormat.RGBA16F, w, h);
            gBufferPositionTexture.minFilter(GPUTexture.MinFilter.NEAREST);
            gBufferPositionTexture.magFilter(GPUTexture.MagFilter.NEAREST);
            gBufferPositionTexture.label("GBuffer Position Texture");

            gBufferNormalTexture = new GPUTexture(GPUTexture.Target.TEXTURE_2D);
            gBufferNormalTexture.storage(1, TextureFormat.R11F_G11F_B10F, w, h);
            gBufferNormalTexture.minFilter(GPUTexture.MinFilter.NEAREST);
            gBufferNormalTexture.magFilter(GPUTexture.MagFilter.NEAREST);
            gBufferNormalTexture.label("GBuffer Normal Texture");

            gBufferColorTexture = new GPUTexture(GPUTexture.Target.TEXTURE_2D);
            gBufferColorTexture.storage(1, TextureFormat.RGBA8, w, h);
            gBufferColorTexture.minFilter(GPUTexture.MinFilter.NEAREST);
            gBufferColorTexture.magFilter(GPUTexture.MagFilter.NEAREST);
            gBufferColorTexture.label("GBuffer Color Texture");

            gBufferMaterialTexture = new GPUTexture(GPUTexture.Target.TEXTURE_2D);
            gBufferMaterialTexture.storage(1, TextureFormat.RGBA16F, w, h);
            gBufferMaterialTexture.minFilter(GPUTexture.MinFilter.NEAREST);
            gBufferMaterialTexture.magFilter(GPUTexture.MagFilter.NEAREST);
            gBufferMaterialTexture.label("GBuffer Material Texture");

            gBufferDepthTexture = new GPUTexture(GPUTexture.Target.TEXTURE_2D);
            gBufferDepthTexture.storage(1, TextureFormat.DEPTH24_STENCIL8, w, h);
            gBufferDepthTexture.minFilter(GPUTexture.MinFilter.NEAREST);
            gBufferDepthTexture.magFilter(GPUTexture.MagFilter.NEAREST);
            gBufferDepthTexture.label("GBuffer Depth Texture");

            fb.texture(gBufferPositionTexture, GPUFramebuffer.Attachment.COLOR0, 0);
            fb.texture(gBufferNormalTexture, GPUFramebuffer.Attachment.COLOR1, 0);
            fb.texture(gBufferColorTexture, GPUFramebuffer.Attachment.COLOR2, 0);
            fb.texture(gBufferMaterialTexture, GPUFramebuffer.Attachment.COLOR3, 0);
            fb.texture(gBufferDepthTexture, GPUFramebuffer.Attachment.DEPTH_STENCIL, 0);

            fb.drawBuffers(new GPUFramebuffer.Attachment[]{GPUFramebuffer.Attachment.COLOR0, GPUFramebuffer.Attachment.COLOR1, GPUFramebuffer.Attachment.COLOR2, GPUFramebuffer.Attachment.COLOR3});
        }, (fb) -> {
            gBufferPositionTexture.destroy();
            gBufferNormalTexture.destroy();
            gBufferColorTexture.destroy();
            gBufferMaterialTexture.destroy();
            gBufferDepthTexture.destroy();
        });
        gFramebuffer.label("GBuffer");

        shadowBlueNoiseTexture = assetManager.loadSync(new AssetIdentifier("bluenoise.png"), GPUTexture.class).get();
        shadowBlueNoiseTexture.wrapS(GPUTexture.Wrap.REPEAT);
        shadowBlueNoiseTexture.wrapT(GPUTexture.Wrap.REPEAT);

//        scene = new Scene();
//
//        god = new Entity();
//        god.addComponent(new PhysxSceneComponent(physics));
//        god.addComponent(new SkyLightComponent(new DirectionalLight(new Vector3f(-2.0f, 4.0f, 1.0f).negate().normalize(), new Vector3f(1.0f, 1.0f, 1.0f))));
//        god.addComponent(new PBRManagerComponent(scene, god));
//
//        Entity floor = new Entity();
//        PxRigidActor actor = PhysxActorCreateMemoryLeakinator.giveMeASuperActor(physics, assetManager, new AssetIdentifier("ground.obj"), CollisionFlags.WORLD.flag(), CollisionFlags.ALL.flag());
//        floor.addComponent(new PhysxActorComponent(floor, actor));
//        ModelRenderer modelRenderer = new ModelRenderer(new AssetIdentifier("ground.glb"));
//        floor.addComponent(modelRenderer);
//
//        god.getComponent(PhysxSceneComponent.class).scene().addActor(actor);
//
//        scene.addRootEntity(god);
//        scene.addRootEntity(floor);
//        scene.load(assetManager);

        scene = assetManager.loadSync(new AssetIdentifier("example_level.json5"), Scene.class).get();
        god = scene.getByUUID(UUID.nameUUIDFromBytes("god".getBytes(StandardCharsets.UTF_8)));
        if (god == null) throw new IllegalArgumentException("fuck");

        camera = new Camera(
                new Vector3f(4.0f, 20.0f, 4.0f),
                new Vector3f(0.0f),
                new Vector3f(0f, 1f, 0f),
                (float) Math.toRadians(60f),
                (float) window.width() / window.height(),
                0.01f,
                1000f
        );
        clusteredShading = new ClusteredShading(god.getComponent(PBRManagerComponent.class).lightSSBO());
        shadowManager = new ShadowManager(camera, shadowResolution, 4, 250f, 0.7f);
        frustumCulling = new FrustumCulling(shadowManager, god.getComponent(SkyLightComponent.class).skylight(), shadowResolution, camera);

        font = new Font(ResourceHelper.loadFileFromResource("Nunito.ttf"), 512, 1, 1024, 1024);

        font.writeAtlasToDisk(new File("font.png"));

        this.player = new Player(physics, camera, window.inputState());
        this.soundManager = new SoundManager(player);
        this.soundSource = new SoundSource();
        this.sound = assetManager.loadSync(new AssetIdentifier("sound.wav"), Sound.class).get();
        if (!sound.valid()) throw new RuntimeException("Sound isn't valid!");
    }

    public Scene scene() {
        return scene;
    }

    private void start() {
        window.setCursorMode(GLFW.GLFW_CURSOR_DISABLED);
        window.show();
        window.focusWindow();
        window.requestAttention();
        this.soundSource.playSound(sound);

        this.pause();

        while (!this.shouldClose()) {
            double time = GLFW.glfwGetTime();
            this.update(frameTime);
            this.render(frameTime);
            this.swap();
            frameTime = GLFW.glfwGetTime() - time;
        }

        this.cleanup();
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
            physics.update((float) fixedDeltaTime, 1);
            player.fixedUpdate(fixedDeltaTime);
            scene.fixedUpdate((float) fixedDeltaTime);

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
            assetManager.unload(new AssetIdentifier("shaders/post.vertex.glsl"), String.class);
            assetManager.unload(new AssetIdentifier("shaders/post.fragment.glsl"), String.class);
            post = new ShaderProgram();
            post.attachShaderSource("Post Vertex Shader", assetManager.loadSync(new AssetIdentifier("shaders/post.vertex.glsl"), String.class).get(), ShaderProgram.ShaderType.VERTEX);
            post.attachShaderSource("Post Fragment Shader", assetManager.loadSync(new AssetIdentifier("shaders/post.fragment.glsl"), String.class).get(), ShaderProgram.ShaderType.FRAGMENT);
            post.link();
            post.label("Post Program");

            post.uniform1i(post.getUniformLocation("img_texture"), 0);
            post.uniform1i(post.getUniformLocation("depth_texture"), 1);
            post.uniform1i(post.getUniformLocation("gbuffer_position"), 2);
            post.uniform1i(post.getUniformLocation("gbuffer_normal"), 3);
            post.uniform1i(post.getUniformLocation("gbuffer_material"), 4);
            post.uniform1i(post.getUniformLocation("blue_noise"), 5);
        }

        if (!paused) {
            fixedUpdate(delta);
            scene.update((float)delta);
            player.update(delta);
            this.soundManager.update();
        }

        if (currentScreen != null) {
            currentScreen.update(delta);
        }
        window.update();

        this.culling().update();

        for (int i = 0; i < passQueries.length; i++) {
            if (passQueries[i].isResultAvailable()) {
                passTimes[i] = passQueries[i].getResult64();
            }
        }
    }

    private void renderShadowPass(double delta) {
        try (StateManager.DebugGroupPopper _ = StateManager.pushDebugGroup(DebugSource.SOURCE_APPLICATION, 1, "Shadow")) {
            try (Query.QueryEnder _ = passQueries[0].begin()) {
                this.shadowManager.bindFramebuffer();

                StateManager.frontFace(FrontFace.CCW);
                StateManager.cullFace(CullFace.BACK);

                StateManager.enable(Capability.CULL_FACE);
                StateManager.enable(Capability.DEPTH_TEST);

                culling().shadow(true);
                shadowManager.prepareShadowPass();
                shadowManager.bindUniforms(null, ShadowManager.getShadowView(god.getComponent(SkyLightComponent.class).skylight()));
                scene.render((float) delta);
                shadowManager.finishShadowPass();
                culling().shadow(false);

                StateManager.disable(Capability.CULL_FACE);
                StateManager.disable(Capability.DEPTH_TEST);
            }
        }
    }

    private void renderGBufferPass(double delta) {
        try (StateManager.DebugGroupPopper _ = StateManager.pushDebugGroup(DebugSource.SOURCE_APPLICATION, 2, "GBuffer")) {
            try (Query.QueryEnder _ = passQueries[1].begin()) {
                StateManager.viewport(0, 0, window.width(), window.height());
                StateManager.clearColor(0.2f, 0.2f, 0.2f, 1.0f);
                gFramebuffer.clear(FramebufferBit.COLOR | FramebufferBit.DEPTH);

                StateManager.frontFace(FrontFace.CCW);
                StateManager.cullFace(CullFace.BACK);

                StateManager.enable(Capability.CULL_FACE);
                StateManager.enable(Capability.DEPTH_TEST);

                camera.bind(0);
                scene.render((float) delta);

                StateManager.disable(Capability.CULL_FACE);
                StateManager.disable(Capability.DEPTH_TEST);
            }
        }
    }

    private void renderGBufferBlit(double delta) {
        try (StateManager.DebugGroupPopper _ = StateManager.pushDebugGroup(DebugSource.SOURCE_APPLICATION, 3, "GBuffer blit")) {
            try (Query.QueryEnder _ = passQueries[2].begin()) {
                gFramebuffer.unbind(GPUFramebuffer.Target.ALL);

                finalFramebuffer.clear(FramebufferBit.COLOR | FramebufferBit.DEPTH);

                camera.bind(0);
                god.getComponent(PBRManagerComponent.class).pbrUBO().bind(1);
                god.getComponent(PBRManagerComponent.class).lightSSBO().bind(2);
                clusteredShading.clusterGridSSBO().bind(3);
                gBufferBlit.use();
                gBufferPositionTexture.bindToUnit(0);
                gBufferNormalTexture.bindToUnit(1);
                gBufferColorTexture.bindToUnit(2);
                gBufferMaterialTexture.bindToUnit(3);
                gBufferDepthTexture.bindToUnit(4);
                shadowBlueNoiseTexture.bindToUnit(5);
                shadowManager.texture().bindToUnit(6);
                gBufferBlit.uniform1f(gBufferBlit.getUniformLocation("z_near"), camera.near());
                gBufferBlit.uniform1f(gBufferBlit.getUniformLocation("z_far"), camera.far());
                gBufferBlit.uniform3i(gBufferBlit.getUniformLocation("grid_size"), ClusteredShading.GRID_SIZE_X, ClusteredShading.GRID_SIZE_Y, ClusteredShading.GRID_SIZE_Z);
                gBufferBlit.uniform2i(gBufferBlit.getUniformLocation("screen_dimensions"), window.width(), window.height());
                shadowManager.bindUniforms(gBufferBlit, ShadowManager.getShadowView(god.getComponent(SkyLightComponent.class).skylight()));
                quad.draw(VertexArray.DrawMode.TRIANGLES);
                gFramebuffer.copyDepthToBuffer(finalFramebuffer, this.window.width(), this.window.height());
            }
        }
    }

    private void renderForwardPass(double delta) {
        try (StateManager.DebugGroupPopper _ = StateManager.pushDebugGroup(DebugSource.SOURCE_APPLICATION, 4, "Forward")) {
            try (Query.QueryEnder _ = passQueries[3].begin()) {
                StateManager.enable(Capability.CULL_FACE);
                StateManager.enable(Capability.DEPTH_TEST);

                // render

                StateManager.disable(Capability.CULL_FACE);
                StateManager.disable(Capability.DEPTH_TEST);
            }
        }
    }

    private void renderPost(double delta) {
        try (StateManager.DebugGroupPopper _ = StateManager.pushDebugGroup(DebugSource.SOURCE_APPLICATION, 5, "Post")) {
            try (Query.QueryEnder _ = passQueries[4].begin()) {
                gFramebuffer.unbind(GPUFramebuffer.Target.ALL);

                camera.bind(0);
                post.use();
                imgTexture.bindToUnit(0);
                imgDepthTexture.bindToUnit(1);
                gBufferPositionTexture.bindToUnit(2);
                gBufferNormalTexture.bindToUnit(3);
                gBufferMaterialTexture.bindToUnit(4);
                shadowBlueNoiseTexture.bindToUnit(5);
                post.uniform1i(post.getUniformLocation("tonemap_mode"), tonemapMode);
                post.uniform2i(post.getUniformLocation("screen_size"), this.window.width(), this.window.height());
                post.uniform1f(post.getUniformLocation("z_near"), this.camera.near());
                post.uniform1f(post.getUniformLocation("z_far"), this.camera.far());
                quad.draw(VertexArray.DrawMode.TRIANGLES);
                finalFramebuffer.copyDepthToBackBuffer(this.window.width(), this.window.height());
            }
        }
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
        try (StateManager.DebugGroupPopper _ = StateManager.pushDebugGroup(DebugSource.SOURCE_APPLICATION, 7, "ImGui")) {
            try (Query.QueryEnder _ = passQueries[6].begin()) {
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
                    if (overlayEnabled && renderDoc != null)
                        renderDoc.maskOverlayBits(RenderDoc.OverlayBits.NONE.bits(), RenderDoc.OverlayBits.NONE.bits());
                    final long backupCurrentContext = GLFW.glfwGetCurrentContext();
                    ImGui.updatePlatformWindows();
                    ImGui.renderPlatformWindowsDefault();
                    GLFW.glfwMakeContextCurrent(backupCurrentContext);
                    if (overlayEnabled && renderDoc != null)
                        renderDoc.maskOverlayBits(RenderDoc.OverlayBits.DEFAULT.bits(), RenderDoc.OverlayBits.DEFAULT.bits());
                }

            }
        }
    }

    private void renderUi(double delta) {
        try (StateManager.DebugGroupPopper _ = StateManager.pushDebugGroup(DebugSource.SOURCE_APPLICATION, 6, "UI")) {
            try (Query.QueryEnder _ = passQueries[5].begin()) {
                StateManager.enable(Capability.DEPTH_TEST);
//                god.getComponent(PhysxSceneComponent.class).scene().renderDebug(this.camera); // this is done here because it renders into the back-buffer, bypassing post-processing.
                StateManager.disable(Capability.DEPTH_TEST);

                StateManager.enable(Capability.BLEND);
                StateManager.blendFunc(BlendParameter.SRC_ALPHA, BlendParameter.ONE_MINUS_SRC_ALPHA);

                float textScale = 1f / 3f;
                textRenderer.drawText(font, String.format("FPS: %.2f", framerate), 5f, this.window.height() - 3 * (font.lineHeight() * textScale), textScale, new Vector3f(1.0f, 0.0f, 0.0f));
                textRenderer.drawText(font, this.window.inputSequenceHandler().getDebugSequence(), 5f, this.window.height() - 4 * (font.lineHeight() * textScale), textScale, new Vector3f(1.0f, 0.0f, 0.0f));
                player.renderDebug(textRenderer, 5f, this.window.height() - 5 * (this.font.lineHeight() * textScale), textScale, delta);

                for (int i = 0; i < passQueries.length; i++) {
                    textRenderer.drawText(font, String.format("Pass %s took %.3fms", intToPassName(i), ((float) this.passTimes[i] / 1e6)), 5f, this.window.height() - (8 + i) * (this.font.lineHeight() * textScale), textScale, new Vector3f(1.0f, 0.0f, 0.0f));
                }

                textRenderer.drawText(font, String.format("Clustered gathering took %.3fms", ((float) clusteredShading.gatherTimeNs() / 1e6)), 5f, this.window.height() - (8 + passQueries.length) * (this.font.lineHeight() * textScale), textScale, new Vector3f(1.0f, 0.0f, 0.0f));
                textRenderer.drawText(font, String.format("Clustered culling took %.3fms", ((float) clusteredShading.cullTimeNs() / 1e6)), 5f, this.window.height() - (9 + passQueries.length) * (this.font.lineHeight() * textScale), textScale, new Vector3f(1.0f, 0.0f, 0.0f));

                if (currentScreen != null) {
                    currentScreen.render(delta);
                }

                StateManager.disable(Capability.BLEND);
            }
        }
    }

    private void render(double delta) {
        try (StateManager.DebugGroupPopper _ = StateManager.pushDebugGroup(DebugSource.SOURCE_APPLICATION, 0, "Frame")) {
            StateManager.clearColor(1.0f, 0.0f, 0.0f, 1.0f); // back-buffer color is bright fucking red so that we know when the framebuffer isn't drawing properly
            StateManager.clear(FramebufferBit.COLOR | FramebufferBit.DEPTH);

            renderShadowPass(delta);

            clusteredShading.compute(camera);

            // draw to framebuffer
            renderGBufferPass(delta);

            renderGBufferBlit(delta);

            renderForwardPass(delta);

            renderPost(delta);

            renderUi(delta);

            renderImGui();
        }
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