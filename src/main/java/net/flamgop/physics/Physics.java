package net.flamgop.physics;

import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.cooking.*;
import physx.geometry.PxConvexMesh;
import physx.geometry.PxTriangleMesh;
import physx.physics.PxPhysics;
import physx.physics.PxScene;
import physx.physics.PxSceneDesc;
import physx.support.PxArray_PxU32;
import physx.support.PxArray_PxVec3;

import java.nio.ByteBuffer;

public class Physics {

    private final PxDefaultAllocator allocator = new PxDefaultAllocator();
    private final PxDefaultErrorCallback errorCallback = new PxDefaultErrorCallback();
    private final PxFoundation foundation;

    private final PxTolerancesScale tolerances = new PxTolerancesScale();
    private final PxPhysics physics;
    private final PxDefaultCpuDispatcher cpuDispatcher;
    private final PxCookingParams cookingParams;

    public Physics(int threads) {
        int version = PxTopLevelFunctions.getPHYSICS_VERSION();
        int versionMajor = version >> 24;
        int versionMinor = (version >> 16) & 0xff;
        int versionMicro = (version >> 8) & 0xff;
        System.out.printf("PhysX loaded, version: %d.%d.%d\n", versionMajor, versionMinor, versionMicro);

        this.foundation = PxTopLevelFunctions.CreateFoundation(version, allocator, errorCallback);

        this.physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerances);

        this.cpuDispatcher = PxTopLevelFunctions.DefaultCpuDispatcherCreate(threads);

        this.cookingParams = new PxCookingParams(tolerances);
    }

    public PxDefaultCpuDispatcher cpuDispatcher() {
        return cpuDispatcher;
    }

    public PxPhysics physics() {
        return physics;
    }

    public PxFoundation foundation() {
        return foundation;
    }

    public PxTolerancesScale tolerances() {
        return tolerances;
    }

    public PxSceneDesc defaultSceneDesc(PxVec3 gravity) {
        PxSceneDesc sceneDesc = new PxSceneDesc(tolerances());
        sceneDesc.setGravity(gravity);
        sceneDesc.setCpuDispatcher(cpuDispatcher());
        sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
        return sceneDesc;
    }

    public PhysicsScene createScene(PxSceneDesc sceneDesc) {
        PxScene pXscene = this.physics.createScene(sceneDesc);
        PhysicsScene scene = new PhysicsScene(pXscene);
        sceneDesc.destroy();
        return scene;
    }

    public PxTriangleMesh loadMesh(ByteBuffer bytes) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            AIScene scene = Assimp.aiImportFileFromMemory(bytes, Assimp.aiProcess_Triangulate | Assimp.aiProcess_JoinIdenticalVertices, (ByteBuffer) null);
            if (scene == null || scene.mNumMeshes() == 0) throw new IllegalStateException("Failed to load mesh");
            AIMesh aiMesh = AIMesh.create(scene.mMeshes().get(0));

            PxArray_PxVec3 vertexBuffer = new PxArray_PxVec3();
            PxArray_PxU32 indexBuffer = new PxArray_PxU32();

            AIVector3D.Buffer vertices = aiMesh.mVertices();
            for (int i = 0; i < aiMesh.mNumVertices(); i++) {
                AIVector3D vertex = vertices.get(i);
                vertexBuffer.pushBack(PxVec3.createAt(stack, MemoryStack::nmalloc, vertex.x(), vertex.y(), vertex.z()));
            }

            AIFace.Buffer faces = aiMesh.mFaces();
            for (int i = 0; i < aiMesh.mNumFaces(); i++) {
                AIFace face = faces.get(i);
                for (int j = 0; j  < face.mNumIndices(); j++) {
                    indexBuffer.pushBack(face.mIndices().get(j));
                }
            }

            PxBoundedData points = PxBoundedData.createAt(stack, MemoryStack::nmalloc);
            points.setCount(vertexBuffer.size());
            points.setStride(PxVec3.SIZEOF);
            points.setData(vertexBuffer.begin());

            PxBoundedData triangles = PxBoundedData.createAt(stack, MemoryStack::nmalloc);
            triangles.setCount(indexBuffer.size() / 3);
            triangles.setStride(4 * 3);
            triangles.setData(indexBuffer.begin());

            PxTriangleMeshDesc meshDesc = new PxTriangleMeshDesc();
            meshDesc.setPoints(points);
            meshDesc.setTriangles(triangles);

            PxTriangleMesh mesh = PxTopLevelFunctions.CreateTriangleMesh(cookingParams, meshDesc);

            scene.free();
            return mesh;
        }
    }

    public PxConvexMesh loadConvexMesh(ByteBuffer bytes) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            AIScene scene = Assimp.aiImportFileFromMemory(bytes, Assimp.aiProcess_Triangulate | Assimp.aiProcess_JoinIdenticalVertices, (ByteBuffer) null);
            if (scene == null || scene.mNumMeshes() == 0) throw new IllegalStateException("Failed to load mesh");
            AIMesh aiMesh = AIMesh.create(scene.mMeshes().get(0));

            PxArray_PxVec3 vertexBuffer = new PxArray_PxVec3();
            PxArray_PxU32 indexBuffer = new PxArray_PxU32();

            AIVector3D.Buffer vertices = aiMesh.mVertices();
            for (int i = 0; i < aiMesh.mNumVertices(); i++) {
                AIVector3D vertex = vertices.get(i);
                vertexBuffer.pushBack(PxVec3.createAt(stack, MemoryStack::nmalloc, vertex.x(), vertex.y(), vertex.z()));
            }

            AIFace.Buffer faces = aiMesh.mFaces();
            for (int i = 0; i < aiMesh.mNumFaces(); i++) {
                AIFace face = faces.get(i);
                for (int j = 0; j  < face.mNumIndices(); j++) {
                    indexBuffer.pushBack(face.mIndices().get(j));
                }
            }

            PxBoundedData points = PxBoundedData.createAt(stack, MemoryStack::nmalloc);
            points.setCount(vertexBuffer.size());
            points.setStride(PxVec3.SIZEOF);
            points.setData(vertexBuffer.begin());

            PxBoundedData triangles = PxBoundedData.createAt(stack, MemoryStack::nmalloc);
            triangles.setCount(indexBuffer.size() / 3);
            triangles.setStride(4 * 3);
            triangles.setData(indexBuffer.begin());

            PxConvexMeshDesc meshDesc = new PxConvexMeshDesc();
            meshDesc.setPoints(points);
            meshDesc.setFlags(new PxConvexFlags((short)(PxConvexFlagEnum.eCOMPUTE_CONVEX.value | PxConvexFlagEnum.eCHECK_ZERO_AREA_TRIANGLES.value | PxConvexFlagEnum.eQUANTIZE_INPUT.value)));

            PxConvexMesh mesh = PxTopLevelFunctions.CreateConvexMesh(cookingParams, meshDesc);

            scene.free();
            return mesh;
        }
    }

    public void destroy() {
        cpuDispatcher.destroy();
        physics.release();
        tolerances.destroy();
        foundation.release();
        errorCallback.destroy();
        allocator.destroy();
    }
}
