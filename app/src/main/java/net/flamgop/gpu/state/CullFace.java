package net.flamgop.gpu.state;

import static org.lwjgl.opengl.GL46.*;

public enum CullFace {
    BACK(GL_BACK),
    FRONT(GL_FRONT),
    FRONT_AND_BACK(GL_FRONT_AND_BACK);;

    final int glQualifier;

    CullFace(int glQualifier) {
        this.glQualifier = glQualifier;
    }
}
