package net.flamgop.asset.loaders;

import net.flamgop.asset.AssetIdentifier;
import net.flamgop.asset.Loader;
import net.flamgop.gpu.GPUTexture;
import net.flamgop.util.ResourceHelper;

public class TextureLoader implements Loader<GPUTexture> {
    @Override
    public GPUTexture load(AssetIdentifier path) {
        return GPUTexture.loadFromBytes(ResourceHelper.loadFileFromAssetsOrResources(path.path()));
    }

    @Override
    public void dispose(GPUTexture asset) {
        asset.destroy();
    }
}
