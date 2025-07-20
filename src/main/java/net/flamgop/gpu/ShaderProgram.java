package net.flamgop.gpu;

import static org.lwjgl.opengl.GL46.*;

public class ShaderProgram {

    private final int handle;

    public ShaderProgram() {
        this.handle = glCreateProgram();
    }

    public void attachShaderSource(String shaderSource, int shaderType) {
        int shader = glCreateShader(shaderType);
        glShaderSource(shader, shaderSource);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException(glGetShaderInfoLog(shader));
        }
        glAttachShader(handle, shader);
        glDeleteShader(shader);
    }

    public void link() {
        glLinkProgram(handle);
        if (glGetProgrami(handle, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Program linking failed:\n" + glGetProgramInfoLog(handle));
        }
    }

    public int getUniformLocation(String uniformName) {
        return glGetUniformLocation(handle, uniformName);
    }

    public int handle() {
        return handle;
    }

    public void use() {
        glUseProgram(handle);
    }

    public void destroy() {
        glDeleteProgram(handle);
    }
}
