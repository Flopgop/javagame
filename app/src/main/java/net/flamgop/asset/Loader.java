package net.flamgop.asset;

public interface Loader<T> {
    T load(AssetIdentifier path);
    void dispose(T asset);
}
