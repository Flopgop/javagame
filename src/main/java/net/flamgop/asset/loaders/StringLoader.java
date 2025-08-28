package net.flamgop.asset.loaders;

import net.flamgop.asset.AssetIdentifier;
import net.flamgop.asset.Loader;
import net.flamgop.util.ResourceHelper;

public class StringLoader implements Loader<String> {
    @Override
    public String load(AssetIdentifier path) {
        return ResourceHelper.loadFileContentsFromAssetsOrResources(path.path());
    }

    @Override
    public void dispose(String asset) {
        // do nothing
    }
}
