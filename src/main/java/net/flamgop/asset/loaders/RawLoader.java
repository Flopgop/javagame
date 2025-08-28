package net.flamgop.asset.loaders;

import net.flamgop.asset.AssetIdentifier;
import net.flamgop.asset.Loader;
import net.flamgop.util.ResourceHelper;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class RawLoader implements Loader<ByteBuffer> {
    @Override
    public ByteBuffer load(AssetIdentifier path) {
        return ResourceHelper.loadFileFromAssetsOrResources(path.path());
    }

    @Override
    public void dispose(ByteBuffer asset) {
        MemoryUtil.memFree(asset);
    }
}
