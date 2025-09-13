package net.flamgop.asset.loaders;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import net.flamgop.asset.AssetIdentifier;
import net.flamgop.asset.AssetManager;
import net.flamgop.asset.Loader;
import net.flamgop.entity.Entity;
import net.flamgop.entity.Scene;
import net.flamgop.entity.components.*;
import net.flamgop.gpu.data.Light;
import net.flamgop.physics.Material;
import net.flamgop.physics.Physics;
import net.flamgop.shadow.DirectionalLight;
import net.flamgop.util.ResourceHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class SceneLoader implements Loader<Scene> {

    private final Json5 json5 = Json5.builder(options -> options.allowInvalidSurrogate().trailingComma().prettyPrinting().build());
    private final AssetManager assetManager;
    private final Physics physics;

    public SceneLoader(AssetManager assetManager, Physics physics) {
        this.assetManager = assetManager;
        this.physics = physics;
    }

    @Override
    public Scene load(AssetIdentifier path) {
        String json = ResourceHelper.loadFileContentsFromAssetsOrResources(path.path());

        Json5Element element = json5.parse(json);
        Json5Object root = element.getAsJson5Object();
        Json5Object pbr = root.getAsJson5Object("pbr");
        Json5Object materials = root.getAsJson5Object("materials");
        Map<String, Material> physicsMaterials = new HashMap<>();
        for (String s : materials.keySet()) {
            Json5Object mat = materials.get(s).getAsJson5Object();
            physicsMaterials.put(s, new Material(
                    mat.get("friction").getAsFloat(),
                    mat.get("restitution").getAsFloat()
            ));
        }

        Scene scene = new Scene();
        Entity god = new Entity(UUID.nameUUIDFromBytes("god".getBytes(StandardCharsets.UTF_8)));
        god.addComponent(new SkyLightComponent(new DirectionalLight(vector3fFromJson(pbr.get("skylight_position")).negate().normalize(), vector3fFromJson(pbr.get("skylight_color")))));
        god.addComponent(new PBRManagerComponent(scene, god));
        scene.addRootEntity(god);

        Json5Array lights = root.getAsJson5Array("lights");
        Json5Array statics = root.getAsJson5Array("static");
        Json5Array dynamics = root.getAsJson5Array("dynamic");

        if (statics != null) {
            for (Json5Element e : statics) {
                Entity ent = new Entity();

                Json5Object stat = e.getAsJson5Object();
                if (stat.has("model")) ent.addComponent(new ModelRenderer(new AssetIdentifier(stat.get("model").getAsString())));
                if (stat.has("position")) ent.transform().position(vector3fFromJson(stat.get("position")));
                if (stat.has("rotation")) ent.transform().rotation(quaternionfFromJson(stat.get("rotation")));
                if (stat.has("collision")) {
                    Material material;
                    if (stat.has("material"))
                        material = physicsMaterials.get(stat.get("material").getAsString());
                    else
                        material = new Material(0.5f, 1.0f);
                    ent.addComponent(new RigidbodyComponent(
                            physics,
                            new AssetIdentifier(stat.get("collision").getAsString()),
                            material
                    ));
                }
                scene.addRootEntity(ent);
            }
        }
        if (dynamics != null) {
            for (Json5Element e : dynamics) {
                Entity ent = new Entity();

                Json5Object stat = e.getAsJson5Object();
                if (stat.has("model")) ent.addComponent(new ModelRenderer(new AssetIdentifier(stat.get("model").getAsString())));
                if (stat.has("position")) ent.transform().position(vector3fFromJson(stat.get("position")));
                if (stat.has("rotation")) ent.transform().rotation(quaternionfFromJson(stat.get("rotation")));
                if (stat.has("collision")) {
                    Material material;
                    if (stat.has("material"))
                        material = physicsMaterials.get(stat.get("material").getAsString());
                    else
                        material = new Material(0.5f, 1.0f);
                    ent.addComponent(new RigidbodyComponent(
                            physics,
                            new AssetIdentifier(stat.get("collision").getAsString()),
                            true,
                            stat.has("mass") ? stat.get("mass").getAsFloat() : 1f,
                            material
                    ));
                }
                scene.addRootEntity(ent);
            }
        }
        if (lights != null) {
            for (Json5Element e : lights) {
                Json5Object jsonLight = e.getAsJson5Object();
                Light light = new Light(
                        jsonLight.has("position") ? vector3fFromJson(jsonLight.get("position")) : new Vector3f(0,0,0),
                        jsonLight.has("color") ? vector3fFromJson(jsonLight.get("color")) : new Vector3f(1,1,1),
                        jsonLight.has("constant") ? jsonLight.get("constant").getAsFloat() : 1.0f,
                        jsonLight.has("linear") ? jsonLight.get("linear").getAsFloat() : 0.7f,
                        jsonLight.has("quadratic") ? jsonLight.get("quadratic").getAsFloat() : 1.8f
                );
                Entity ent = new Entity();
                ent.addComponent(new LightComponent(light));
                scene.addRootEntity(ent);
            }
            god.getComponent(PBRManagerComponent.class).recollectLights();
        }

        scene.load(assetManager);

        return scene;
    }

    @Override
    public void dispose(Scene asset) {
//        asset.destroy();
        // todo: unleak scene lmao
    }

    private Vector3f vector3fFromJson(Json5Element element) {
        Json5Array array = element.getAsJson5Array();
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private Quaternionf quaternionfFromJson(Json5Element element) {
        Json5Array array = element.getAsJson5Array();
        return new Quaternionf(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat(), array.get(3).getAsFloat());
    }
}
