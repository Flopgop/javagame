package net.flamgop.gpu.model;

import net.flamgop.asset.AssetLoader;
import net.flamgop.gpu.DefaultShaders;
import net.flamgop.gpu.GPUTexture;
import net.flamgop.gpu.VertexArray;
import net.flamgop.util.Util;
import org.lwjgl.assimp.*;

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

        Material material = Material.MISSING_MATERIAL;
        GPUTexture texture = null;

        if (mesh.mMaterialIndex() >= 0) {
            AIMaterial aiMaterial = AIMaterial.create(scene.mMaterials().get(mesh.mMaterialIndex()));

            AIString path = AIString.calloc();
            int[] texIndex = new int[1];
            Assimp.aiGetMaterialTexture(aiMaterial, Assimp.aiTextureType_BASE_COLOR, 0, path, null, texIndex, null, null, null, null);
            String texturePath = path.dataString();
            path.free();

            if (!texturePath.isEmpty()) {
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
        }

        if (texture != null) {
            material = new Material(DefaultShaders.GBUFFER, texture);
        }

        return new TexturedMesh(vao, material);
    }

    public void draw() {
        for (TexturedMesh mesh : meshes) {
            mesh.draw();
        }
    }
}
