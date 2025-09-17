package net.flamgop.asset;

import org.jetbrains.annotations.NotNull;

public record AssetIdentifier(String path) {
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AssetIdentifier(String path1)) {
            return path1.equals(this.path());
        }
        return false;
    }

    @NotNull
    @Override
    public String toString() {
        return path;
    }
}
