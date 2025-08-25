package net.flamgop.level;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import net.flamgop.asset.AssetLoader;
import net.flamgop.level.json.JsonDynamicEntity;
import net.flamgop.level.json.JsonLight;
import net.flamgop.level.json.JsonStaticMesh;
import net.flamgop.physics.Physics;
import org.joml.Vector3f;

public class LevelLoader {

    private final Json5 json5 = Json5.builder(options ->
            options.allowInvalidSurrogate().trailingComma().prettyPrinting().build());
    private final AssetLoader assetLoader;

    public LevelLoader(AssetLoader assetLoader) {
        this.assetLoader = assetLoader;
    }

    public Level load(Physics physics, String json) {
        Json5Element element = json5.parse(json);
        Json5Object root = element.getAsJson5Object();
        Json5Object pbr = root.getAsJson5Object("pbr");
        Json5Array statics = root.getAsJson5Array("static");
        Json5Array dynamics = root.getAsJson5Array("dynamic");
        Json5Array lights = root.getAsJson5Array("lights");

        Level level = new Level(physics);

        if (statics != null) {
            for (Json5Element meshElem : statics) {
                Json5Object mesh = meshElem.getAsJson5Object();
                JsonStaticMesh staticMesh = new JsonStaticMesh();
                staticMesh.identifier = mesh.get("id").getAsString();
                if (mesh.has("model")) {
                    staticMesh.modelIdentifier = mesh.get("model").getAsString();
                } else {
                    staticMesh.modelIdentifier = null;
                }
                staticMesh.position = jsonArrayToFloatArray(mesh.get("position").getAsJson5Array());
                staticMesh.rotation = jsonArrayToFloatArray(mesh.get("rotation").getAsJson5Array());
                staticMesh.collisionModelIdentifier = mesh.get("collision").getAsString();
                staticMesh.collidesWithFlag = mesh.get("collides_with_flag").getAsInt();
                staticMesh.collisionGroup = mesh.get("collision_group").getAsInt();
                level.staticMesh(assetLoader, staticMesh);
            }
        }
        if (dynamics != null) {
            for (Json5Element entityElem : dynamics) {
                Json5Object entity = entityElem.getAsJson5Object();
                JsonDynamicEntity dynamicEntity = new JsonDynamicEntity();
                dynamicEntity.identifier = entity.get("id").getAsString();
                dynamicEntity.modelIdentifier = entity.get("model").getAsString();
                dynamicEntity.position = jsonArrayToFloatArray(entity.get("position").getAsJson5Array());
                dynamicEntity.rotation = jsonArrayToFloatArray(entity.get("rotation").getAsJson5Array());
                dynamicEntity.collisionModelIdentifier = entity.get("collision").getAsString();
                dynamicEntity.mass = entity.get("mass").getAsFloat();
                dynamicEntity.collidesWithFlag = entity.get("collides_with_flag").getAsInt();
                dynamicEntity.collisionGroup = entity.get("collision_group").getAsInt();
                level.dynamicEntity(assetLoader, dynamicEntity);
            }
        }
        if (lights != null) {
            for (Json5Element lightElem : lights) {
                Json5Object jsonLight = lightElem.getAsJson5Object();
                JsonLight light = new JsonLight();
                light.position = jsonArrayToFloatArray(jsonLight.get("position").getAsJson5Array());
                light.color = jsonArrayToFloatArray(jsonLight.get("color").getAsJson5Array());
                light.constant = jsonLight.get("constant").getAsFloat();
                light.linear = jsonLight.get("linear").getAsFloat();
                light.quadratic = jsonLight.get("quadratic").getAsFloat();
                level.light(light);
            }
        }

        level.configurePBRData(vector3fFromJson(pbr.get("skylight_position")), vector3fFromJson(pbr.get("skylight_color")));
        return level;
    }

    private Vector3f vector3fFromJson(Json5Element element) {
        Json5Array array = element.getAsJson5Array();
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private float[] jsonArrayToFloatArray(Json5Array jsonArray) {
        float[] result = new float[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            result[i] = jsonArray.get(i).getAsFloat();
        }
        return result;
    }
}
