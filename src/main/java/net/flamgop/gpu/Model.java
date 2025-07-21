package net.flamgop.gpu;

import net.flamgop.util.Util;
import org.lwjgl.assimp.*;

import java.util.ArrayList;
import java.util.List;

public class Model {

    public final List<VertexArray> meshes = new ArrayList<>();

    @SuppressWarnings("DataFlowIssue")
    public void load(String path) {
        AIScene scene = Assimp.aiImportFile(path, Assimp.aiProcess_Triangulate | Assimp.aiProcess_FlipUVs);
        if (scene == null || (scene.mFlags() & Assimp.AI_SCENE_FLAGS_INCOMPLETE) != 0 || scene.mRootNode() == null) {
            throw new IllegalStateException("Failed to load model");
        }
        processNode(scene.mRootNode(), scene);
    }

    private void processNode(AINode node, AIScene scene) {
        for (int i = 0; i < node.mNumMeshes(); i++) {
            AIMesh mesh = AIMesh.create(scene.mMeshes().get(node.mMeshes().get(i)));
            meshes.add(processMesh(mesh, scene));
        }
        for (int i = 0; i < node.mNumChildren(); i++) {
            processNode(AINode.create(node.mChildren().get(i)), scene);
        }
    }

    private VertexArray processMesh(AIMesh mesh, AIScene scene) {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<GPUTexture> textures = new ArrayList<>();

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

        if (mesh.mMaterialIndex() >= 0) {
            AIMaterial material = AIMaterial.create(scene.mMaterials().get(mesh.mMaterialIndex()));
            System.out.println(material);
        }
        return VertexArray.withDefaultVertexFormat(Util.doubleToFloatArray(vertices.stream().mapToDouble(f -> f).toArray()), indices.stream().mapToInt(i -> i).toArray());
    }

    public void draw(ShaderProgram shader) {
        shader.use();
        for (VertexArray buffer : meshes) {
            buffer.draw();
        }
    }
}
