package net.flamgop.gpu.model;

import net.flamgop.asset.AssetLoader;
import net.flamgop.gpu.DefaultShaders;
import net.flamgop.gpu.GPUTexture;
import net.flamgop.gpu.VertexArray;
import net.flamgop.util.Util;
import org.jetbrains.annotations.Nullable;
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
        AIScene scene = Assimp.aiImportFileFromMemory(assetLoader.load(identifier), Assimp.aiProcess_Triangulate | Assimp.aiProcess_FlipUVs, (ByteBuffer) null);
        if (scene == null || (scene.mFlags() & Assimp.AI_SCENE_FLAGS_INCOMPLETE) != 0 || scene.mRootNode() == null) {
            throw new IllegalStateException("Failed to load model");
        }
        processNode(assetLoader, scene.mRootNode(), scene);
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
        }

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

            for (int i = Assimp.aiTextureType_NONE; i < Assimp.aiTextureType_MAYA_SPECULAR_ROUGHNESS + 1; i++) {
                int count = Assimp.aiGetMaterialTextureCount(aiMaterial, i);
                if (count > 0)
                    System.out.println("Object has " + count + " textures of type " + Assimp.aiTextureTypeToString(i));
            }

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

        return new TexturedMesh(vao, material);
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
                    texture = GPUTexture.loadFromBytes(assetLoader.load("file:" + texturePath));
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

    public void draw() {
        for (TexturedMesh mesh : meshes) {
            mesh.draw();
        }
    }
}
