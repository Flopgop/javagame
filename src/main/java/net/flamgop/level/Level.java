package net.flamgop.level;

import net.flamgop.asset.AssetLoader;
import net.flamgop.gpu.uniform.Light;
import net.flamgop.gpu.uniform.LightArray;
import net.flamgop.level.json.JsonDynamicEntity;
import net.flamgop.level.json.JsonLight;
import net.flamgop.level.json.JsonStaticMesh;
import net.flamgop.physics.Physics;
import net.flamgop.physics.PhysicsScene;
import org.joml.Vector3f;
import physx.common.PxVec3;

import java.util.ArrayList;
import java.util.List;

public class Level {
    private final LightArray lightArray = new LightArray();
    private final List<StaticMesh> staticMeshes = new ArrayList<>();
    private final List<DynamicEntity> dynamicEntities = new ArrayList<>();

    private final Physics physics;
    private final PhysicsScene scene;

    public Level(Physics physics) {
        this.physics = physics;
        PxVec3 temp = new PxVec3();
        temp.setX(0); temp.setY(2*-9.81f); temp.setZ(0);
        this.scene = physics.createScene(physics.defaultSceneDesc(temp));
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

    public void staticMesh(AssetLoader assetLoader, JsonStaticMesh jsonMesh) {
        StaticMesh mesh = StaticMesh.fromJson(assetLoader, physics, jsonMesh);
        mesh.addToScene(scene);
        staticMeshes.add(mesh);
    }

    public void dynamicEntity(AssetLoader assetLoader, JsonDynamicEntity jsonEntity) {
        DynamicEntity entity = DynamicEntity.fromJson(assetLoader, physics, jsonEntity);
        entity.addToScene(scene);
        dynamicEntities.add(entity);
    }

    public LightArray lights() {
        return lightArray;
    }

    public void update(double delta) {
        for (DynamicEntity entity : dynamicEntities) {
//            entity.update(delta);
            // specific behavior TBD
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
}
