package net.flamgop.physics;

import net.flamgop.gpu.Camera;
import net.flamgop.gpu.ShaderProgram;
import net.flamgop.util.PhysxJoml;
import net.flamgop.util.ResourceHelper;
import org.joml.Vector3f;
import physx.PxTopLevelFunctions;
import physx.character.PxControllerManager;
import physx.common.*;
import physx.physics.*;
import physx.support.PxVisualizationParameterEnum;

import java.util.concurrent.ConcurrentLinkedDeque;

import static org.lwjgl.opengl.GL46.*;

public class PhysicsScene {
    private final PxScene scene;
    private final PxControllerManager controllerManager;

    private float gravity = -9.81f;
    private final PxVec3 tmpVec = new PxVec3(0.0f, gravity, 0.0f);

    public PhysicsScene(PxScene scene) {
        this.scene = scene;

        this.controllerManager = PxTopLevelFunctions.CreateControllerManager(scene);
    }

    public PxControllerManager controllerManager() {
        return controllerManager;
    }

    public PxScene handle() {
        return scene;
    }

    public void addActor(PxActor actor) {
        scene.addActor(actor);
    }

    public float gravity() {
        return gravity;
    }

    public RaycastHit raycast(Vector3f start, Vector3f direction, float maxDistance) {
        PxHitFlags hitFlags = new PxHitFlags((short) PxHitFlagEnum.eDEFAULT.value);
        PxFilterData filterData = new PxFilterData();
        filterData.setWord0(CollisionFlags.RAYCAST.flag());
        filterData.setWord1(CollisionFlags.ALL.flag());
        PxQueryFlags queryFlags = new PxQueryFlags((short) (PxQueryFlagEnum.eSTATIC.value | PxQueryFlagEnum.eDYNAMIC.value));
        PxQueryFilterData queryFilterData = new PxQueryFilterData();
        queryFilterData.setData(filterData);
        queryFilterData.setFlags(queryFlags);
        PxVec3 origin = PhysxJoml.toPxVec3(start);
        PxVec3 dir = PhysxJoml.toPxVec3(direction);
        dir.normalize();

        PxRaycastBuffer10 raycastBuffer = new PxRaycastBuffer10();
        boolean hit = this.scene.raycast(origin, dir, maxDistance, raycastBuffer, hitFlags, queryFilterData);

        PxRaycastHit closest = null;
        if (raycastBuffer.getNbAnyHits() > 0) {
            closest = raycastBuffer.getAnyHit(0);
            for (int i = 1; i < raycastBuffer.getNbAnyHits(); i++) {
                PxRaycastHit next = raycastBuffer.getAnyHit(i);
                if (closest.getDistance() > next.getDistance()) {
                    closest = next;
                }
            }
        }

        RaycastHit result = new RaycastHit(
                hit && closest != null,
                hit && closest != null ? new RaycastHit.RaycastData(
                        closest.getActor(),
                        PhysxJoml.toVector3f(closest.getPosition()),
                        PhysxJoml.toVector3f(closest.getNormal())
                ) : null
        );

        raycastBuffer.destroy();
        origin.destroy();
        dir.destroy();
        queryFilterData.destroy();
        queryFlags.destroy();
        filterData.destroy();
        hitFlags.destroy();
        return result;
    }

    public void gravity(float gravity) {
        this.gravity = gravity;
        tmpVec.setX(0);
        tmpVec.setY(gravity);
        tmpVec.setZ(0);
        scene.setGravity(tmpVec);
    }

    public void fixedUpdate(double delta) {
        scene.simulate((float) delta);
        scene.fetchResults(true);
    }

    private boolean debugEnabled = false;

    private ShaderProgram pointsProgram;
    private int pointsVAO;
    private int pointsVBO;

    private ShaderProgram linesProgram;
    private int lineVAO;
    private int lineVBO;

    private ShaderProgram trianglesProgram;
    private int trianglesVAO;
    private int trianglesVBO;

    private void initVAOAndVBO(int vao, int vbo) {
        glNamedBufferData(vbo, 0, GL_STREAM_DRAW);

        int pointsStride = 6 * Float.BYTES;
        glVertexArrayVertexBuffer(vao, 0, vbo, 0, pointsStride);

        glEnableVertexArrayAttrib(vao, 0);
        glVertexArrayAttribFormat(vao, 0, 3, GL_FLOAT, false, 0);
        glVertexArrayAttribBinding(vao, 0, 0);

        glEnableVertexArrayAttrib(vao, 1);
        glVertexArrayAttribFormat(vao, 1, 3, GL_FLOAT, false, 3 * Float.BYTES);
        glVertexArrayAttribBinding(vao, 1, 0);
    }

    public void setupDebug() {
        if (debugEnabled) return;
        debugEnabled = true;

        String vertex = ResourceHelper.loadFileContentsFromResource("shaders/physx_debug.vertex.glsl");
        String fragment = ResourceHelper.loadFileContentsFromResource("shaders/physx_debug.fragment.glsl");

        pointsProgram = new ShaderProgram();
        // this uses a custom vertex for gl_PointSize
        pointsProgram.attachShaderSource("Points Debug Vertex Shader", ResourceHelper.loadFileContentsFromResource("shaders/physx_debug_points.vertex.glsl"), GL_VERTEX_SHADER);
        pointsProgram.attachShaderSource("Points Debug Fragment Shader", fragment, GL_FRAGMENT_SHADER);
        pointsProgram.link();
        pointsProgram.label("Points Debug Program");

        linesProgram = new ShaderProgram();
        linesProgram.attachShaderSource("Lines Debug Vertex Shader", vertex, GL_VERTEX_SHADER);
        linesProgram.attachShaderSource("Lines Debug Fragment Shader", fragment, GL_FRAGMENT_SHADER);
        linesProgram.link();
        linesProgram.label("Lines Debug Program");

        trianglesProgram = new ShaderProgram();
        trianglesProgram.attachShaderSource("Triangles Debug Vertex Shader", vertex, GL_VERTEX_SHADER);
        trianglesProgram.attachShaderSource("Triangles Debug Fragment Shader", fragment, GL_FRAGMENT_SHADER);
        trianglesProgram.link();
        trianglesProgram.label("Triangles Debug Program");

        pointsVAO = glCreateVertexArrays();
        pointsVBO = glCreateBuffers();
        initVAOAndVBO(pointsVAO, pointsVBO);

        lineVAO = glCreateVertexArrays();
        lineVBO = glCreateBuffers();
        initVAOAndVBO(lineVAO, lineVBO);

        trianglesVAO = glCreateVertexArrays();
        trianglesVBO = glCreateBuffers();
        initVAOAndVBO(trianglesVAO, trianglesVBO);

        scene.setVisualizationParameter(PxVisualizationParameterEnum.eSCALE, 1.0f);
        scene.setVisualizationParameter(PxVisualizationParameterEnum.eCOLLISION_SHAPES, 1.0f);
        scene.setVisualizationParameter(PxVisualizationParameterEnum.eCOLLISION_EDGES, 1.0f);
        scene.setVisualizationParameter(PxVisualizationParameterEnum.eCOLLISION_AABBS, 1.0f);
        scene.setVisualizationParameter(PxVisualizationParameterEnum.eACTOR_AXES, 1.0f);
    }

    private void debugPushVertexBaseIndex(float[] dst, int baseVertex, PxVec3 pos, int color) {
        dst[baseVertex] = pos.getX();
        dst[baseVertex+1] = pos.getY();
        dst[baseVertex+2] = pos.getZ();
        dst[baseVertex+3] = ((color >> 16) & 0xFF) / 255.0f;
        dst[baseVertex+4] = ((color >> 8) & 0xFF) / 255.0f;
        dst[baseVertex+5] = ((color & 0xFF) / 255.0f);
    }

    public void renderDebug(Camera camera) {
        if (!debugEnabled) return;
        camera.bind(0);

        glEnable(GL_LINE_SMOOTH);
        glLineWidth(1.25f);

        PxRenderBuffer rb = this.scene.getRenderBuffer();

        int numPoints = rb.getNbPoints();
        if (numPoints > 0) {
            float[] pointVertices = new float[numPoints * 6];

            PxDebugPoint points = rb.getPoints();
            for (int i = 0; i < numPoints; i++) {
                PxDebugPoint point = PxDebugPoint.arrayGet(points.getAddress(), i);

                debugPushVertexBaseIndex(pointVertices, i * 6, point.getPos(), point.getColor());
            }

            glNamedBufferData(pointsVBO, pointVertices, GL_STREAM_DRAW);

            glBindVertexArray(pointsVAO);
            pointsProgram.use();
            glDrawArrays(GL_POINTS, 0, numPoints);
        }

        int numLines = rb.getNbLines();
        if (numLines > 0) {
            float[] lineVertices = new float[numLines * 2 * 6];

            PxDebugLine lines = rb.getLines();
            for (int i = 0; i < numLines; i++) {
                PxDebugLine line = PxDebugLine.arrayGet(lines.getAddress(), i);

                int base = i * 2 * 6;

                debugPushVertexBaseIndex(lineVertices, base, line.getPos0(), line.getColor0());
                debugPushVertexBaseIndex(lineVertices, base + 6, line.getPos1(), line.getColor1());
            }

            glNamedBufferData(lineVBO, lineVertices, GL_STREAM_DRAW);

            glBindVertexArray(lineVAO);
            linesProgram.use();
            glDrawArrays(GL_LINES, 0, numLines * 2);
        }

        int numTris = rb.getNbTriangles();
        if (numTris > 0) {
            float[] triangleVertices = new float[numTris * 3 * 6];

            PxDebugTriangle tris = rb.getTriangles();
            for (int i = 0; i < numTris; i++) {
                PxDebugTriangle tri = PxDebugTriangle.arrayGet(tris.getAddress(), i);

                int base = i * 3 * 6;

                debugPushVertexBaseIndex(triangleVertices, base, tri.getPos0(), tri.getColor0());
                debugPushVertexBaseIndex(triangleVertices, base + 6, tri.getPos1(), tri.getColor1());
                debugPushVertexBaseIndex(triangleVertices, base + 12, tri.getPos2(), tri.getColor2());
            }

            glNamedBufferData(trianglesVBO, triangleVertices, GL_STREAM_DRAW);

            glBindVertexArray(trianglesVAO);
            trianglesProgram.use();
            glDrawArrays(GL_TRIANGLES, 0, numTris * 3);
        }

        glDisable(GL_LINE_SMOOTH);
    }
}
