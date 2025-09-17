package net.flamgop.asset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SuppressWarnings("unchecked")
public class AssetManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssetManager.class);

    private final Map<AssetIdentifier, Asset<?>> cache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Loader<?>> loaders = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public <T> void registerLoader(Class<T> type, Loader<T> loader) {
        loaders.put(type, loader);
    }

    public void track(AssetIdentifier identifier, Asset<?> asset) {
        if (!cache.containsKey(identifier)) {
            asset.retain();
            cache.put(identifier, asset);
        }
    }

    public boolean isTracking(AssetIdentifier identifier) {
        return cache.containsKey(identifier);
    }

    public <T> Asset<T> get(AssetIdentifier identifier) {
        if (!this.isTracking(identifier)) return null;
        Asset<T> asset = (Asset<T>) cache.get(identifier);
        asset.retain();
        return asset;
    }

    public <T> Asset<T> loadSync(AssetIdentifier path, Class<T> type) {
        Asset<?> cached = cache.get(path);
        if (cached != null) {
            cached.retain();
            return (Asset<T>) cached;
        }

        Loader<T> loader = (Loader<T>) loaders.get(type);
        if (loader == null) throw new RuntimeException("No loader registered for " + type);

        LOGGER.info("Loading {}...", path);

        T data = loader.load(path);
        if (data == null) return null;
        Asset<T> asset = new Asset<>(data);
        cache.put(path, asset);
        return asset;
    }

    public <T> Future<Asset<T>> loadAsync(AssetIdentifier path, Class<T> type) {
        return executor.submit(() -> loadSync(path, type));
    }

    public <T> void unload(AssetIdentifier path, Class<T> type) {
        Asset<?> asset = cache.get(path);
        if (asset == null) return;

        if (asset.release()) {
            cache.remove(path);
            Loader<T> loader = (Loader<T>) loaders.get(type);
            loader.dispose((T) asset.get());
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
