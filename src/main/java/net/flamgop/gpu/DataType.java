package net.flamgop.gpu;

import static org.lwjgl.opengl.GL46.*;

public enum DataType {
    BYTE(GL_BYTE),
    UNSIGNED_BYTE(GL_UNSIGNED_BYTE),
    SHORT(GL_SHORT),
    UNSIGNED_SHORT(GL_UNSIGNED_SHORT),
    INT(GL_INT),
    UNSIGNED_INT(GL_UNSIGNED_INT),
    FLOAT(GL_FLOAT),
    TWO_BYTES(GL_2_BYTES),
    THREE_BYTES(GL_3_BYTES),
    FOUR_BYTES(GL_4_BYTES),
    DOUBLE(GL_DOUBLE),

    ;
    final int glQualifier;
    DataType(final int glQualifier) {
        this.glQualifier = glQualifier;
    }
    public int glQualifier() {
        return glQualifier;
    }
}
