package net.flamgop.asset.loaders;

import net.flamgop.asset.*;
import net.flamgop.gpu.DefaultShaders;
import net.flamgop.gpu.GPUTexture;
import net.flamgop.gpu.Vertex;
import net.flamgop.gpu.VertexArray;
import net.flamgop.gpu.model.Material;
import net.flamgop.gpu.model.Model;
import net.flamgop.gpu.model.TexturedMesh;
import net.flamgop.util.AABB;
import net.flamgop.util.ResourceHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.assimp.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL46.*;

@SuppressWarnings("DataFlowIssue")
public class ModelLoader implements Loader<Model> {

    private static final int IMPORT_FLAGS =
            Assimp.aiProcess_Triangulate |
                    Assimp.aiProcess_FlipUVs |
                    Assimp.aiProcess_CalcTangentSpace |
                    Assimp.aiProcess_JoinIdenticalVertices |
                    Assimp.aiProcess_GenSmoothNormals |
                    Assimp.aiProcess_ImproveCacheLocality |
                    Assimp.aiProcess_FixInfacingNormals |
                    Assimp.aiProcess_FindDegenerates;

    private final AssetManager assetManager;

    public ModelLoader(final AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Override
    public Model load(AssetIdentifier path) {
        String p = path.path();

        ByteBuffer asset = ResourceHelper.loadFileFromAssetsOrResources(p);

        List<TexturedMesh> meshes = new ArrayList<>();

        AIScene scene = Assimp.aiImportFileFromMemory(asset, IMPORT_FLAGS, (ByteBuffer) null);
        if (scene == null || (scene.mFlags() & Assimp.AI_SCENE_FLAGS_INCOMPLETE) != 0 || scene.mRootNode() == null) {
            throw new IllegalStateException("Failed to load model");
        }
        processNode(p, meshes, scene.mRootNode(), scene);

        Assimp.aiReleaseImport(scene);

        return new Model(meshes);
    }

    private void processNode(String basePath, List<TexturedMesh> meshes, AINode node, AIScene scene) {
        for (int i = 0; i < node.mNumMeshes(); i++) {
            AIMesh mesh = AIMesh.create(scene.mMeshes().get(node.mMeshes().get(i)));
            meshes.add(processMesh(basePath, mesh, scene));
        }
        for (int i = 0; i < node.mNumChildren(); i++) {
            processNode(basePath, meshes, AINode.create(node.mChildren().get(i)), scene);
        }
    }

    private TexturedMesh processMesh(String basePath, AIMesh mesh, AIScene scene) {
        List<Vertex> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < mesh.mNumVertices(); i++) {
            Vertex v = new Vertex();
            AIVector3D vertex = mesh.mVertices().get(i);
            v.position(vertex.x(), vertex.y(), vertex.z());
            AIVector3D normal = mesh.mNormals().get(i);
            v.normal(normal.x(), normal.y(), normal.z());
            if (mesh.mTextureCoords(0) != null) {
                AIVector3D texcoord = mesh.mTextureCoords(0).get(i);
                v.texcoord(texcoord.x(), texcoord.y());
            }
            AIVector3D tangent = mesh.mTangents().get(i);
            AIVector3D bitangent = mesh.mBitangents().get(i);

            Vector3f n = new Vector3f(normal.x(), normal.y(), normal.z());
            Vector3f t = new Vector3f(tangent.x(), tangent.y(), tangent.z());
            Vector3f b = new Vector3f(bitangent.x(), bitangent.y(), bitangent.z());

            float w = (t.cross(b).dot(n) > 0.0f ? 1.0f : -1.0f);

            v.tangent(tangent.x(), tangent.y(), tangent.z(), (int)w);
            vertices.add(v);

            if (vertex.x() < minX) minX = vertex.x();
            if (vertex.x() > maxX) maxX = vertex.x();
            if (vertex.y() < minY) minY = vertex.y();
            if (vertex.y() > maxY) maxY = vertex.y();
            if (vertex.z() < minZ) minZ = vertex.z();
            if (vertex.z() > maxZ) maxZ = vertex.z();
        }
        AABB aabb = new AABB(new Vector3f(minX, minY, minZ), new Vector3f(maxX, maxY, maxZ));

        for (int i = 0; i < mesh.mNumFaces(); i++) {
            AIFace face = mesh.mFaces().get(i);
            for (int j = 0; j  < face.mNumIndices(); j++) {
                indices.add(face.mIndices().get(j));
            }
        }

        VertexArray vao = VertexArray.withDefaultVertexFormat(
                vertices.toArray(new Vertex[0]),
                indices.stream().mapToInt(i -> i).toArray()
        );

        boolean named = mesh.mName().length() > 0;
        if (named) {
            vao.label(mesh.mName().dataString());
        }

        Material material;
        GPUTexture diffuse = null;
        GPUTexture roughness = null;
        GPUTexture metallic = null;
        GPUTexture normal = null;

        if (mesh.mMaterialIndex() >= 0) {
            AIMaterial aiMaterial = AIMaterial.create(scene.mMaterials().get(mesh.mMaterialIndex()));

            diffuse = loadTexture(basePath, scene, aiMaterial, Assimp.aiTextureType_BASE_COLOR);
            if (diffuse == null) diffuse = loadTexture(basePath, scene, aiMaterial, Assimp.aiTextureType_DIFFUSE);
            roughness = loadTexture(basePath, scene, aiMaterial, Assimp.aiTextureType_DIFFUSE_ROUGHNESS);
            metallic = loadTexture(basePath, scene, aiMaterial, Assimp.aiTextureType_METALNESS);
            normal = loadTexture(basePath, scene, aiMaterial, Assimp.aiTextureType_NORMALS);
            if (normal == null) normal = loadTexture(basePath, scene, aiMaterial, Assimp.aiTextureType_NORMAL_CAMERA);
        }

        float maxAniso = glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY);
        if (roughness != null) {
            roughness.minFilter(GPUTexture.MinFilter.LINEAR_MIPMAP_LINEAR);
            roughness.magFilter(GPUTexture.MagFilter.LINEAR);
            roughness.maxAnisotropy(maxAniso);

        }
        if (metallic != null) {
            metallic.minFilter(GPUTexture.MinFilter.LINEAR_MIPMAP_LINEAR);
            metallic.magFilter(GPUTexture.MagFilter.LINEAR);
            metallic.maxAnisotropy(maxAniso);
        }
        if (normal != null) {
            normal.minFilter(GPUTexture.MinFilter.LINEAR_MIPMAP_LINEAR);
            normal.magFilter(GPUTexture.MagFilter.LINEAR);
            normal.maxAnisotropy(maxAniso);
        }

        material = new Material(DefaultShaders.GBUFFER,
                diffuse != null ? diffuse : GPUTexture.MISSING_TEXTURE,
                roughness != null ? roughness : GPUTexture.MISSING_TEXTURE,
                metallic != null ? metallic : GPUTexture.MISSING_TEXTURE,
                normal != null ? normal : GPUTexture.MISSING_NORMAL
        );

        return new TexturedMesh(vao, material, aabb, aabb.center(), aabb.radius());
    }

    private @Nullable GPUTexture loadTexture(String sourcePath, AIScene scene, AIMaterial material, int aiTextureType) {
        int count = Assimp.aiGetMaterialTextureCount(material, aiTextureType);
        if (count <= 0) return null;

        GPUTexture texture = null;
        String name = null;

        AIString path = AIString.calloc();
        int[] texIndex = new int[1];
        Assimp.aiGetMaterialTexture(material, aiTextureType, 0, path, null, texIndex, null, null, null, null);
        String texturePath = path.dataString();
        path.free();

        if (!texturePath.isEmpty()) {
            name = texturePath;
            if (texturePath.startsWith("*")) {
                int id = Integer.parseInt(texturePath.substring(1));
                AITexture aiTexture = AITexture.create(scene.mTextures().get(id));

                AssetIdentifier pathIdentifier = new AssetIdentifier(sourcePath + "/" + texturePath);
                if (!assetManager.isTracking(pathIdentifier)) {
                    texture = TextureLoader.loadFromAssimpTexture(aiTexture);

                    Asset<GPUTexture> asset = new Asset<>(texture);
                    assetManager.track(pathIdentifier, asset);
                } else {
                    texture = assetManager.<GPUTexture>get(pathIdentifier).get();
                }
            } else {
                System.out.println("Loading texture: " + texturePath);
                texture = assetManager.loadSync(new AssetIdentifier(texturePath), GPUTexture.class).get();
            }
        } else if (texIndex[0] >= 0) {
            AITexture aiTexture = AITexture.create(scene.mTextures().get(texIndex[0]));
            if (aiTexture.mHeight() == 0) {
                AssetIdentifier pathIdentifier = new AssetIdentifier(sourcePath + "/*" + texIndex[0]);
                if (!assetManager.isTracking(pathIdentifier)) {
                    texture = TextureLoader.loadFromAssimpTexture(aiTexture);

                    Asset<GPUTexture> asset = new Asset<>(texture);
                    assetManager.track(pathIdentifier, asset);
                } else {
                    texture = assetManager.<GPUTexture>get(pathIdentifier).get();
                }
            } else {
                System.out.println("Image data is uncompressed, I have no idea what to do with this!");
            }
        }

        if (name != null)
            texture.label("Texture \"" + name + "\"");

        return texture;
    }

    @Override
    public void dispose(Model asset) {
        asset.destroy();
    }
}
