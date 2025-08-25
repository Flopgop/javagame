package net.flamgop.asset;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public record AssetKey(AssetType type, String path) {

    public static @NotNull AssetKey fromString(String string) throws IllegalArgumentException {
        String standard = string.toLowerCase();
        int colonIndex = standard.indexOf(":");
        if (colonIndex == -1) throw new IllegalArgumentException("Asset key \"" + string + "\" does not follow valid format of \"type:path\".");
        String type = standard.substring(0, colonIndex);
        String filePath = string.substring(colonIndex + 1);

        return new AssetKey(AssetType.valueOf(type.toUpperCase(Locale.US)), filePath);
    }

    @NotNull
    @Override
    public String toString() {
        return type + ":" + path;
    }
}
