package net.flamgop.level;

import net.flamgop.asset.AssetManager;
import net.flamgop.gpu.buffer.GPUBuffer;
import net.flamgop.gpu.buffer.ShaderStorageBuffer;
import net.flamgop.gpu.buffer.UniformBuffer;
import net.flamgop.gpu.uniform.Light;
import net.flamgop.gpu.uniform.LightArray;
import net.flamgop.gpu.uniform.PBRUniformData;
import net.flamgop.level.json.JsonDynamicEntity;
import net.flamgop.level.json.JsonLight;
import net.flamgop.level.json.JsonStaticMesh;
import net.flamgop.physics.Physics;
import net.flamgop.physics.PhysicsScene;
import net.flamgop.shadow.DirectionalLight;
import net.flamgop.util.AABB;
import org.joml.Vector3f;
import org.joml.Vector4f;
import physx.common.PxVec3;

import java.util.ArrayList;
import java.util.List;

public class Level {
    private final LightArray lightArray = new LightArray();
    private final List<StaticMesh> staticMeshes = new ArrayList<>();
    private final List<DynamicEntity> dynamicEntities = new ArrayList<>();

    private final Physics physics;
    private final PhysicsScene scene;

    private final UniformBuffer pbrUniformBuffer;
    private final ShaderStorageBuffer lightSSBO;

    private final DirectionalLight skylight;

    public Level(Physics physics) {
        this.physics = physics;
        PxVec3 temp = new PxVec3();
        temp.setX(0); temp.setY(2*-9.81f); temp.setZ(0);
        this.scene = physics.createScene(physics.defaultSceneDesc(temp));

        pbrUniformBuffer = new UniformBuffer(GPUBuffer.UpdateHint.STATIC);
        pbrUniformBuffer.buffer().label("PBR UBO");

        lightSSBO = new ShaderStorageBuffer(GPUBuffer.UpdateHint.STATIC);
        lightSSBO.buffer().label("Light SSBO");
        skylight = new DirectionalLight();
    }

    //ColorUtil.getRGBFromK(5900)
    public void configurePBRData(Vector3f sunPos, Vector3f sunColor) {
        PBRUniformData pbr = new PBRUniformData();
        pbr.ambient = new Vector4f(new Vector3f(0.1f, 0.3f, 0.6f).normalize(), 0.5f);
        pbr.lightColor = new Vector4f(new Vector3f(sunColor).normalize(), 25f);
        pbr.lightDirection = new Vector3f(sunPos).negate().normalize();
        pbr.lightCount = this.lights().lights.size();
        pbrUniformBuffer.allocate(pbr);

        lightSSBO.allocate(this.lights());

        skylight.direction = new Vector3f(sunPos).negate().normalize();
        skylight.color = new Vector3f(sunColor);
    }

    public PhysicsScene scene() {
        return scene;
    }

    public void light(Light light) {
        lightArray.lights.add(light);
    }

    public void staticMesh(StaticMesh mesh) {
        staticMeshes.add(mesh);
    }

    public void dynamicEntity(DynamicEntity entity) {
        dynamicEntities.add(entity);
    }

    public void light(JsonLight jsonLight) {
        Light light = new Light(
                new Vector3f(jsonLight.position[0], jsonLight.position[1], jsonLight.position[2]),
                new Vector3f(jsonLight.color[0], jsonLight.color[1], jsonLight.color[2]),
                jsonLight.constant, jsonLight.linear, jsonLight.quadratic
        );
        lightArray.lights.add(light);
    }

    public void staticMesh(AssetManager assetManager, JsonStaticMesh jsonMesh) {
        StaticMesh mesh = StaticMesh.fromJson(assetManager, physics, jsonMesh);
        mesh.addToScene(scene);
        staticMeshes.add(mesh);
    }

    public void dynamicEntity(AssetManager assetManager, JsonDynamicEntity jsonEntity) {
        DynamicEntity entity = DynamicEntity.fromJson(assetManager, physics, jsonEntity);
        entity.addToScene(scene);
        dynamicEntities.add(entity);
    }

    public LightArray lights() {
        return lightArray;
    }

    public UniformBuffer pbrUniformBuffer() {
        return pbrUniformBuffer;
    }

    public ShaderStorageBuffer lightSSBO() {
        return lightSSBO;
    }

    public DirectionalLight skylight() {
        return skylight;
    }

    public void update(double delta) {
        for (DynamicEntity entity : dynamicEntities) {
            entity.update(delta);
        }
    }

    public void render(double delta) {
        for (StaticMesh mesh : staticMeshes) {
            mesh.render(delta);
        }
        for (DynamicEntity entity : dynamicEntities) {
            entity.render(delta);
        }
    }

    public List<AABB> getAllObjectBounds() {
        List<AABB> aabbs = new ArrayList<>();
        for (DynamicEntity entity : dynamicEntities) {
            if (entity.model() != null)
                entity.model().meshes.forEach(tm -> aabbs.add(tm.aabb()));
        }
        for (StaticMesh mesh : staticMeshes) {
            if (mesh.model() != null)
                mesh.model().meshes.forEach(tm -> aabbs.add(tm.aabb()));
        }
        return aabbs;
    }
}
