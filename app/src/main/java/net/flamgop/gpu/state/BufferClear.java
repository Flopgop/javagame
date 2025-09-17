package net.flamgop.gpu.state;

public record BufferClear(int color, int depth, int stencil) {
    public BufferClear(boolean color) {
        this(color, false, false);
    }

    public BufferClear(boolean color, boolean depth) {
        this(color, depth, false);
    }

    public BufferClear(boolean color, boolean depth, boolean stencil) {
        this(color ? 1 : 0, depth ? 1 : 0, stencil ? 1 : 0);
    }
}
