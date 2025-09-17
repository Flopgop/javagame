package net.flamgop.gpu.state;

import static org.lwjgl.opengl.GL46.*;

public enum FrontFace {
    CW(GL_CW),
    CCW(GL_CCW),

    ;
    final int glQualifier;
    FrontFace(int glQualifier) {
        this.glQualifier = glQualifier;
    }
}
