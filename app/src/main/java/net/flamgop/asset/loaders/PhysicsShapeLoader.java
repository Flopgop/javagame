package net.flamgop.asset.loaders;

import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.readonly.Vec3Arg;
import net.flamgop.asset.AssetIdentifier;
import net.flamgop.asset.Loader;
import net.flamgop.physics.PhysicsShape;
import net.flamgop.util.ResourceHelper;
import org.lwjgl.assimp.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class PhysicsShapeLoader implements Loader<PhysicsShape> {
    @Override
    public PhysicsShape load(AssetIdentifier path) {
        ByteBuffer bytes = ResourceHelper.loadFileFromAssetsOrResources(path.path());
        try (AIScene scene = Assimp.aiImportFileFromMemory(bytes, Assimp.aiProcess_Triangulate | Assimp.aiProcess_JoinIdenticalVertices, (ByteBuffer) null)) {
            if (scene == null || scene.mNumMeshes() == 0) throw new IllegalStateException("Failed to load mesh");
            try (AIMesh aiMesh = AIMesh.create(scene.mMeshes().get(0))) {
                List<Vec3Arg> vertexBuffer = new ArrayList<>();

                AIVector3D.Buffer vertices = aiMesh.mVertices();
                for (int i = 0; i < aiMesh.mNumVertices(); i++) {
                    AIVector3D vertex = vertices.get(i);
                    vertexBuffer.add(new Vec3(vertex.x(), vertex.y(), vertex.z()));
                }

                return new PhysicsShape(vertexBuffer);
            }
        }
    }

    @Override
    public void dispose(PhysicsShape asset) {
        asset.destroy();
    }
}
