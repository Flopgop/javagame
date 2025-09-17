package net.flamgop.gpu.state;

import static org.lwjgl.opengl.GL43.GL_DEBUG_SOURCE_THIRD_PARTY;
import static org.lwjgl.opengl.GL46.GL_DEBUG_SOURCE_APPLICATION;

public enum DebugSource {
    SOURCE_APPLICATION(GL_DEBUG_SOURCE_APPLICATION),
    SOURCE_THIRD_PARTY(GL_DEBUG_SOURCE_THIRD_PARTY),

    ;
    final int glQualifier;
    DebugSource(int glQualifier) {
        this.glQualifier = glQualifier;
    }
}
