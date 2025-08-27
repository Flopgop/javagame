package net.flamgop.gpu.model;

import net.flamgop.Game;
import net.flamgop.asset.AssetKey;
import net.flamgop.asset.AssetLoader;
import net.flamgop.asset.AssetType;
import net.flamgop.gpu.DefaultShaders;
import net.flamgop.gpu.GPUTexture;
import net.flamgop.gpu.Vertex;
import net.flamgop.gpu.VertexArray;
import net.flamgop.util.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.assimp.*;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL46.*;

@SuppressWarnings("DataFlowIssue")
public class Model {

    private static final int IMPORT_FLAGS =
            Assimp.aiProcess_Triangulate |
            Assimp.aiProcess_FlipUVs |
            Assimp.aiProcess_CalcTangentSpace |
            Assimp.aiProcess_JoinIdenticalVertices |
            Assimp.aiProcess_GenSmoothNormals |
            Assimp.aiProcess_ImproveCacheLocality |
            Assimp.aiProcess_FixInfacingNormals |
            Assimp.aiProcess_FindDegenerates;

    public final List<TexturedMesh> meshes = new ArrayList<>();

    @SuppressWarnings("DataFlowIssue")
    public void load(AssetLoader assetLoader, String identifier) throws FileNotFoundException {
        System.out.println("Loading model: " + identifier);
        AIScene scene = Assimp.aiImportFileFromMemory(assetLoader.load(AssetKey.fromString(identifier)), IMPORT_FLAGS, (ByteBuffer) null);
        if (scene == null || (scene.mFlags() & Assimp.AI_SCENE_FLAGS_INCOMPLETE) != 0 || scene.mRootNode() == null) {
            throw new IllegalStateException("Failed to load model");
        }
        processNode(assetLoader, scene.mRootNode(), scene);

        Assimp.aiReleaseImport(scene);
    }

    private void processNode(AssetLoader assetLoader, AINode node, AIScene scene) {
        for (int i = 0; i < node.mNumMeshes(); i++) {
            AIMesh mesh = AIMesh.create(scene.mMeshes().get(node.mMeshes().get(i)));
            meshes.add(processMesh(assetLoader, mesh, scene));
        }
        for (int i = 0; i < node.mNumChildren(); i++) {
            processNode(assetLoader, AINode.create(node.mChildren().get(i)), scene);
        }
    }

    private TexturedMesh processMesh(AssetLoader assetLoader, AIMesh mesh, AIScene scene) {
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

            diffuse = loadTexture(assetLoader, scene, aiMaterial, Assimp.aiTextureType_BASE_COLOR);
            if (diffuse == null) diffuse = loadTexture(assetLoader, scene, aiMaterial, Assimp.aiTextureType_DIFFUSE);
            roughness = loadTexture(assetLoader, scene, aiMaterial, Assimp.aiTextureType_DIFFUSE_ROUGHNESS);
            metallic = loadTexture(assetLoader, scene, aiMaterial, Assimp.aiTextureType_METALNESS);
            normal = loadTexture(assetLoader, scene, aiMaterial, Assimp.aiTextureType_NORMALS);
            if (normal == null) normal = loadTexture(assetLoader, scene, aiMaterial, Assimp.aiTextureType_NORMAL_CAMERA);
        }

        float maxAniso = glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY);
        if (roughness != null) {
            glTextureParameteri(roughness.handle(), GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTextureParameteri(roughness.handle(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTextureParameterf(metallic.handle(), GL_TEXTURE_MAX_ANISOTROPY, maxAniso);

        }
        if (metallic != null) {
            glTextureParameteri(metallic.handle(), GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTextureParameteri(metallic.handle(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTextureParameterf(metallic.handle(), GL_TEXTURE_MAX_ANISOTROPY, maxAniso);
        }
        if (normal != null) {
            glTextureParameteri(metallic.handle(), GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTextureParameteri(metallic.handle(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTextureParameterf(metallic.handle(), GL_TEXTURE_MAX_ANISOTROPY, maxAniso);
        }

        material = new Material(DefaultShaders.GBUFFER,
                diffuse != null ? diffuse : GPUTexture.MISSING_TEXTURE,
                roughness != null ? roughness : GPUTexture.MISSING_TEXTURE,
                metallic != null ? metallic : GPUTexture.MISSING_TEXTURE,
                normal != null ? normal : GPUTexture.MISSING_NORMAL
                );

        return new TexturedMesh(vao, material, aabb, aabb.center(), aabb.radius());
    }

    private @Nullable GPUTexture loadTexture(AssetLoader assetLoader, AIScene scene, AIMaterial material, int aiTextureType) {
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
                texture = GPUTexture.loadFromAssimpTexture(aiTexture);
            } else {
                try {
                    System.out.println("Loading texture: " + texturePath);
                    texture = GPUTexture.loadFromBytes(assetLoader.load(new AssetKey(AssetType.FILE, texturePath)));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (texIndex[0] >= 0) {
            AITexture aiTexture = AITexture.create(scene.mTextures().get(texIndex[0]));
            if (aiTexture.mHeight() == 0) {
                texture = GPUTexture.loadFromAssimpTexture(aiTexture);
            } else {
                System.out.println("Image data is uncompressed, I have no idea what to do with this!");
            }
        }

        if (name != null)
            texture.label("Texture \"" + name + "\"");

        return texture;
    }

    public void draw(Matrix4f model) {
        for (TexturedMesh mesh : meshes) {
            if (!Game.INSTANCE.culling().isVisible(mesh, model)) continue;
            mesh.draw();
        }
    }
}
