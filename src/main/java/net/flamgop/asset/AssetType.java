package net.flamgop.asset;

public enum AssetType {
    RESOURCE("resource"),
    FILE("file"),

    ;

    private final String type;
    AssetType(final String type) {
        this.type = type;
    }


    @Override
    public String toString() {
        return this.type;
    }
}
