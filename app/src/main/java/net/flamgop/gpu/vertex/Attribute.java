package net.flamgop.gpu.vertex;

import static org.lwjgl.opengl.GL46.*;

public record Attribute(Type type, int size, boolean normalized, int bindingIndex, int bindingDivisor) {

    public static Attribute of(Type type, int size, boolean normalized) {
        return of(type, size, normalized, 0, 0);
    }

    public static Attribute of(Type type, int size, boolean normalized, int bindingIndex, int bindingDivisor) {
        return new Attribute(type, size, normalized, bindingIndex, bindingDivisor);
    }

    public static final int BGRA = GL_BGRA;

    public enum Type {
        BYTE(GL_BYTE, 1, false),
        SHORT(GL_SHORT, 2, false),
        INT(GL_INT, 4, false),
        FIXED(GL_FIXED, 4, false),
        FLOAT(GL_FLOAT, 4, false),
        HALF_FLOAT(GL_HALF_FLOAT, 2, false),
        DOUBLE(GL_DOUBLE, 8, false),

        UNSIGNED_BYTE(GL_UNSIGNED_BYTE, 1, false),
        UNSIGNED_SHORT(GL_UNSIGNED_SHORT, 2, false),
        UNSIGNED_INT(GL_UNSIGNED_INT, 4, false),

        INT_2_10_10_10_REV(GL_INT_2_10_10_10_REV, 4, true),
        UNSIGNED_INT_2_10_10_10_REV(GL_UNSIGNED_INT_2_10_10_10_REV, 4, true),

        UNSIGNED_INT_10F_11F_11F_REV(GL_UNSIGNED_INT_10F_11F_11F_REV, 4, true),

        ;
        final int glQualifier;
        final int byteCount;
        final boolean packed;

        /**
         * @param glQualifier OpenGL name
         * @param byteCount number of bytes per element
         * @param packed whether to multiply the byteCount by the size of the attribute or not, necessary for INT_2_10_10_10_REV, it's unsigned variant, and UNSIGNED_INT_10F_11F_11F_REV.
         */
        Type(int glQualifier, int byteCount, boolean packed) {
            this.glQualifier = glQualifier;
            this.byteCount = byteCount;
            this.packed = packed;
        }

        public int byteCount() {
            return byteCount;
        }

        public int glQualifier() {
            return glQualifier;
        }

        public boolean packed() {
            return packed;
        }
    }

    public Attribute(Type type, int size, boolean normalized, int bindingIndex, int bindingDivisor) {
        this.type = type;
        this.size = size;
        this.normalized = normalized;
        this.bindingIndex = bindingIndex;
        this.bindingDivisor = bindingDivisor;

        if ((this.size > 4 || this.size < 0) && this.size != BGRA)
            throw new IllegalArgumentException("Size must be between 0 and 4 or Attribute.BGRA.");
        if (type == Type.UNSIGNED_INT_10F_11F_11F_REV && size != 3)
            throw new IllegalArgumentException("UNSIGNED_INT_10F_11F_11F_REV requires size to be 3.");
        if ((type == Type.INT_2_10_10_10_REV || type == Type.UNSIGNED_INT_2_10_10_10_REV) && (size != 4 && size != BGRA))
            throw new IllegalArgumentException("INT_2_10_10_10_REV and UNSIGNED_INT_2_10_10_10_REV requires size to be 4 or Attribute.BGRA.");
        if (size == BGRA && !normalized)
            throw new IllegalArgumentException("Attribute.BGRA requires this attribute to be normalized.");
    }
}
