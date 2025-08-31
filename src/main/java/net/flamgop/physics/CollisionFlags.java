package net.flamgop.physics;

public enum CollisionFlags {
    WORLD(0x01),
    PLAYER(0x02),
    RAYCAST(0x04),

    ALL(WORLD.flag() | PLAYER.flag() | RAYCAST.flag());
    ;

    final int flag;
    CollisionFlags(int flag) {
        this.flag = flag;
    }

    public int flag() {
        return flag;
    }
}
