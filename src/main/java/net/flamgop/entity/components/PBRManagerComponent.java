package net.flamgop.entity.components;

import net.flamgop.asset.AssetManager;
import net.flamgop.entity.AbstractComponent;
import net.flamgop.entity.Entity;
import net.flamgop.entity.Scene;
import net.flamgop.gpu.buffer.GPUBuffer;
import net.flamgop.gpu.buffer.ShaderStorageBuffer;
import net.flamgop.gpu.buffer.UniformBuffer;
import net.flamgop.gpu.data.LightArray;
import net.flamgop.gpu.data.PBRUniformData;
import net.flamgop.shadow.DirectionalLight;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class PBRManagerComponent extends AbstractComponent {

    private final Scene scene;

    private final LightArray lightArray;
    private final ShaderStorageBuffer lightSSBO;
    private final UniformBuffer pbrUBO;

    public PBRManagerComponent(Scene scene, Entity entity) {
        this.scene = scene;
        DirectionalLight skylight = entity.getComponent(SkyLightComponent.class).skylight();

        this.lightArray = new LightArray();
        this.lightSSBO = new ShaderStorageBuffer(GPUBuffer.UpdateHint.STATIC);
        this.lightSSBO.buffer().label("Light SSBO");

        this.pbrUBO = new UniformBuffer(GPUBuffer.UpdateHint.STATIC);
        this.pbrUBO.buffer().label("PBR UBO");

        PBRUniformData pbr = new PBRUniformData();
        pbr.ambient = new Vector4f(new Vector3f(0.1f, 0.3f, 0.6f).normalize(), 0.5f);
        pbr.lightColor = new Vector4f(skylight.color.normalize(new Vector3f()), 25f);
        pbr.lightDirection = new Vector3f(skylight.direction);
        pbr.lightCount = this.lightArray.lights.size();
        pbrUBO.allocate(pbr);

        lightSSBO.allocate(this.lightArray);
    }

    public void recollectLights() {
        this.lightArray.lights.clear();
        scene.allEntities().forEach(e -> {
            if (e.hasComponent(LightComponent.class))
                this.lightArray.lights.add(e.getComponent(LightComponent.class).light());
        });
        lightSSBO.allocate(this.lightArray);
    }

    public UniformBuffer pbrUBO() {
        return pbrUBO;
    }

    public ShaderStorageBuffer lightSSBO() {
        return lightSSBO;
    }

    @Override public void load(AssetManager assetManager) {}
    @Override public void unload(AssetManager assetManager) {}
    @Override public void update(float delta) {}
    @Override public void physicsUpdate(float fixedDelta) {}
    @Override public void render() {}
}
