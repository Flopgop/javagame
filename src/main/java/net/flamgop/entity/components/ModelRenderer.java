package net.flamgop.entity.components;

import net.flamgop.asset.Asset;
import net.flamgop.asset.AssetIdentifier;
import net.flamgop.asset.AssetManager;
import net.flamgop.entity.Component;
import net.flamgop.gpu.buffer.GPUBuffer;
import net.flamgop.gpu.buffer.UniformBuffer;
import net.flamgop.gpu.data.ModelUniformData;
import net.flamgop.gpu.model.Model;

public class ModelRenderer extends Component {

    private final AssetIdentifier assetIdentifier;
    private UniformBuffer uniformBuffer;
    private ModelUniformData modelUniformData;
    private Asset<Model> model;

    public ModelRenderer(AssetIdentifier modelIdentifier) {
        this.assetIdentifier = modelIdentifier;
    }

    public Model model() {
        if (model == null) return null;
        return model.get();
    }

    @Override
    public void load(AssetManager assetManager) {
        this.model = assetManager.loadSync(this.assetIdentifier, Model.class);
        uniformBuffer = new UniformBuffer(GPUBuffer.UpdateHint.STATIC);
        uniformBuffer.buffer().label(this.assetIdentifier.path() + " Model UBO");
        modelUniformData = new ModelUniformData();
        modelUniformData.model = this.transform().getWorldMatrix();
        modelUniformData.computeNormal();
        uniformBuffer.allocate(modelUniformData);
    }

    @Override
    public void unload(AssetManager assetManager) {
        uniformBuffer.destroy();
        assetManager.unload(this.assetIdentifier, Model.class);
    }

    @Override
    public void render() {
        if (model == null) return;
        if (modelUniformData.model != this.transform().getWorldMatrix()) {
            modelUniformData.model = this.transform().getWorldMatrix();
            modelUniformData.computeNormal();
            uniformBuffer.store(modelUniformData);
        }
        uniformBuffer.bind(1);
        model.get().draw(modelUniformData.model);
    }
}
