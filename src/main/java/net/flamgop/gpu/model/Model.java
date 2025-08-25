package net.flamgop.gpu.model;

import net.flamgop.Game;
import net.flamgop.asset.AssetKey;
import net.flamgop.asset.AssetLoader;
import net.flamgop.asset.AssetType;
import net.flamgop.gpu.DefaultShaders;
import net.flamgop.gpu.GPUTexture;
import net.flamgop.gpu.VertexArray;
import net.flamgop.util.AABB;
import net.flamgop.util.Util;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.assimp.*;
import org.lwjgl.opengl.GL46;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("DataFlowIssue")
public class Model {

    public final List<TexturedMesh> meshes = new ArrayList<>();

    @SuppressWarnings("DataFlowIssue")
    public void load(AssetLoader assetLoader, String identifier) throws FileNotFoundException {
        System.out.println("Loading model: " + identifier);
        AIScene scene = Assimp.aiImportFileFromMemory(assetLoader.load(AssetKey.fromString(identifier)), Assimp.aiProcess_Triangulate | Assimp.aiProcess_FlipUVs, (ByteBuffer) null);
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
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < mesh.mNumVertices(); i++) {
            AIVector3D vertex = mesh.mVertices().get(i);
            vertices.add(vertex.x());
            vertices.add(vertex.y());
            vertices.add(vertex.z());
            AIVector3D normal = mesh.mNormals().get(i);
            vertices.add(normal.x());
            vertices.add(normal.y());
            vertices.add(normal.z());
            if (mesh.mTextureCoords(0) != null) {
                AIVector3D texcoord = mesh.mTextureCoords(0).get(i);
                vertices.add(texcoord.x());
                vertices.add(texcoord.y());
            } else {
                vertices.add(0f);
                vertices.add(0f);
            }

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
                Util.doubleToFloatArray(vertices.stream().mapToDouble(f -> f).toArray()),
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

        if (mesh.mMaterialIndex() >= 0) {
            AIMaterial aiMaterial = AIMaterial.create(scene.mMaterials().get(mesh.mMaterialIndex()));

            diffuse = loadTexture(assetLoader, scene, aiMaterial, Assimp.aiTextureType_BASE_COLOR);
            if (diffuse == null) diffuse = loadTexture(assetLoader, scene, aiMaterial, Assimp.aiTextureType_DIFFUSE);
            roughness = loadTexture(assetLoader, scene, aiMaterial, Assimp.aiTextureType_DIFFUSE_ROUGHNESS);
            metallic = loadTexture(assetLoader, scene, aiMaterial, Assimp.aiTextureType_METALNESS);
        }

        if (roughness != null) {
            GL46.glTextureParameteri(roughness.handle(), GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_LINEAR);
            GL46.glTextureParameteri(roughness.handle(), GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_LINEAR);
        }
        if (metallic != null) {
            GL46.glTextureParameteri(metallic.handle(), GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_LINEAR);
            GL46.glTextureParameteri(metallic.handle(), GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_LINEAR);
        }

        material = new Material(DefaultShaders.GBUFFER,
                diffuse != null ? diffuse : GPUTexture.MISSING_TEXTURE,
                roughness != null ? roughness : GPUTexture.MISSING_TEXTURE,
                metallic != null ? metallic : GPUTexture.MISSING_TEXTURE
                );
        System.out.println("Creating material with " + (diffuse != null ? "present" : "missing") + " diffuse, " + (roughness != null ? "present" : "missing") + " roughness, and " + (metallic != null ? "present" : "missing") + " metallic.");

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
