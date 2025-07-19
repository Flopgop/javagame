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
        textRenderer.drawTextWrapped(font,
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nulla imperdiet enim vel ligula aliquet, nec varius nulla consectetur. Duis at euismod elit. Fusce urna mauris, pretium sit amet laoreet viverra, elementum vel sem. Vivamus porttitor scelerisque varius. Nam eu arcu accumsan, euismod metus non, egestas sapien. Donec nec augue leo. Fusce in erat vitae lorem aliquet venenatis vel ut neque. Donec sagittis pellentesque turpis sed tempus. Aliquam erat volutpat. Aenean malesuada, eros ac bibendum blandit, purus diam placerat enim, eu pharetra diam eros et massa. Aenean dui ipsum, tincidunt ut accumsan quis, tincidunt sed felis. Curabitur libero risus, aliquet sed tincidunt in, viverra eget justo. Nunc rhoncus leo a diam fermentum consequat. Nunc finibus blandit convallis. Curabitur ornare ornare est, vel efficitur lorem dapibus id." +
                " Suspendisse ac purus non ante vulputate posuere eget vel mi. Sed in neque sit amet neque bibendum tristique. Vivamus a ultricies erat, eget dignissim nulla. Pellentesque laoreet tellus ac molestie lacinia. Ut quis purus tortor. Morbi nisi tellus, egestas sed porttitor at, blandit sit amet velit. In vitae velit lorem. Fusce quis mattis nibh. Praesent gravida dolor eu tempor vulputate. Vivamus egestas mauris quis tempus consequat. Integer elementum mollis magna, in suscipit purus congue vulputate. Ut aliquet volutpat consectetur. Etiam dignissim congue magna, in pulvinar lectus pharetra quis." +
                " Mauris quis facilisis neque. Pellentesque in urna sit amet dui tempus lacinia. Morbi facilisis luctus turpis, eu commodo lacus vestibulum ac. Nulla in quam facilisis, placerat enim at, scelerisque arcu. Aliquam egestas interdum mauris in ullamcorper. Fusce efficitur felis sit amet eros ornare, sodales tempor dui venenatis. Mauris malesuada egestas laoreet. In elementum libero a pretium aliquet." +
                " Duis a ante eu magna laoreet dignissim. Vivamus et vulputate urna. Nulla sed massa posuere, elementum dui et, consequat urna. Quisque et vulputate risus, nec imperdiet nisl. Fusce egestas sed diam in convallis. Etiam elementum sagittis enim, eget pulvinar nulla fringilla ac. Donec ornare, leo at aliquet porttitor, lorem risus fringilla tortor, in pretium tellus quam nec est. Duis venenatis erat nec mi mollis ornare. Aliquam non metus sed libero accumsan eleifend in et quam. Quisque in diam ut metus pharetra commodo. Ut ultricies, massa at volutpat congue, neque sem ullamcorper dui, et pharetra neque arcu sed tellus." +
                " Interdum et malesuada fames ac ante ipsum primis in faucibus. Vestibulum mollis at urna eget malesuada. In hac habitasse platea dictumst. In maximus rutrum lectus a hendrerit. Praesent ultrices risus et metus faucibus, nec gravida ex finibus. Nulla pharetra bibendum justo mattis lacinia. Vivamus a elementum felis. Pellentesque ante ante, finibus lobortis finibus eget, aliquet eu enim. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Pellentesque consequat et augue vitae imperdiet. Phasellus hendrerit posuere velit ac mattis. Fusce augue turpis, interdum nec ante eu, tincidunt placerat elit. In ullamcorper sagittis ultricies." +
                " Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Pellentesque sagittis ipsum varius, posuere purus eu, aliquam dolor. Curabitur blandit ipsum nec augue tincidunt lacinia. Etiam neque est, blandit id condimentum vitae, tincidunt a ligula. Lorem ipsum dolor sit amet, consectetur adipiscing elit. In ultricies blandit venenatis. Donec tempus odio quis ex varius varius. Aliquam sollicitudin nibh vitae efficitur facilisis. Nam vel libero sed nulla pellentesque scelerisque. Duis consectetur efficitur arcu ut rutrum. Vestibulum eleifend justo quis dui euismod mollis. Morbi maximus auctor volutpat. Lorem ipsum dolor sit amet, consectetur adipiscing elit." +
                " Vivamus scelerisque ante id massa dictum, et auctor risus vulputate. Curabitur non convallis nisl. Suspendisse potenti. Morbi eleifend sit amet lacus id accumsan. Cras mollis accumsan imperdiet. Nam aliquam, dui quis cursus consequat, lacus mauris dignissim sapien, et interdum lacus elit quis lectus. Suspendisse potenti. Nam feugiat metus ac aliquam dapibus. Nullam ut lacinia turpis, gravida rhoncus urna. Etiam fermentum tincidunt nisl, ut aliquet felis ullamcorper sed. Mauris ut augue at enim iaculis dignissim. Mauris metus est, varius in purus eu, congue rutrum purus. Vestibulum aliquam pellentesque lacus sed semper." +
                " Suspendisse sed erat nec sapien rutrum ultricies hendrerit nec ipsum. Curabitur sollicitudin, neque non ultrices mattis, augue ligula ultrices nisi, in fermentum dolor eros id risus. Donec consequat accumsan mi, sit amet ornare nulla vulputate in. Suspendisse bibendum sapien eget massa condimentum, non porttitor eros viverra. Cras sed porttitor velit, quis iaculis urna. Maecenas diam sem, ultricies in vehicula sit amet, commodo at est. Vivamus imperdiet luctus erat, sed tincidunt neque finibus a. Donec mattis dui vitae lectus vestibulum, id interdum nibh volutpat. Vivamus nec gravida ex. Nulla facilisi. Ut feugiat tincidunt ligula, eget iaculis diam tempor id. Duis ultrices feugiat lacus, eu convallis leo ultrices at. Sed quis convallis ipsum. Praesent dignissim, orci nec ornare fermentum, erat felis ornare nibh, et vestibulum libero orci eget nibh." +
                " Vivamus nec vestibulum ipsum. Integer vehicula et tellus vitae sollicitudin. Nunc id quam et risus scelerisque scelerisque vel quis est. Cras nec odio dignissim, volutpat turpis et, tempus lorem. Nullam ante leo, luctus id gravida eget, maximus vitae tellus. Phasellus ut turpis ipsum. Proin sagittis magna vel eleifend auctor. Aenean porta lacus justo, quis mattis erat rhoncus non. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Cras nec dictum est. Donec urna neque, aliquet ac lectus in, posuere elementum ex. Integer venenatis gravida sapien, eu bibendum ligula. Aliquam vehicula mi erat." +
                " Phasellus lobortis est non massa bibendum gravida. Nullam dignissim vehicula tincidunt. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nam sed augue turpis. Mauris erat orci, dictum accumsan consequat quis, consequat quis mi. Etiam eget diam diam. Pellentesque quis tellus quis mauris molestie sollicitudin pharetra quis arcu. ", 0, 720, 0.5f, new Vector3f(0f, 1.0f, 0f), 1280);
        textRenderer.drawText(font, String.format("FPS: %.2f", 1.0 / delta), 25f, 75f, 0.5f, new Vector3f(1.0f, 0.0f, 0.0f));
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