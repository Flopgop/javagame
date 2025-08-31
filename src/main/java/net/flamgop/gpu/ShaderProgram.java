package net.flamgop.gpu;

import org.joml.*;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL46.*;

public class ShaderProgram {

    private final int handle;

    public ShaderProgram() {
        this.handle = glCreateProgram();
    }

    public void attachShaderSource(String name, String shaderSource, int shaderType) {
        int shader = glCreateShader(shaderType);
        glShaderSource(shader, shaderSource);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException(glGetShaderInfoLog(shader));
        }
        glAttachShader(handle, shader);
        glObjectLabel(GL_SHADER, shader, name);
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

    public void uniform1f(int location, float v) {
        glProgramUniform1f(handle, location, v);
    }

    public void uniform2f(int location, float v1, float v2) {
        glProgramUniform2f(handle, location, v1, v2);
    }

    public void uniform2f(int location, Vector2f v) {
        glProgramUniform2f(handle, location, v.x(), v.y());
    }

    public void uniform3f(int location, float v1, float v2, float v3) {
        glProgramUniform3f(handle, location, v1, v2, v3);
    }

    public void uniform3f(int location, Vector3f v) {
        glProgramUniform3f(handle, location, v.x(), v.y(), v.z());
    }

    public void uniform4f(int location, float v1, float v2, float v3, float v4) {
        glProgramUniform4f(handle, location, v1, v2, v3, v4);
    }

    public void uniform4f(int location, Vector4f v) {
        glProgramUniform4f(handle, location, v.x(), v.y(), v.z(), v.w());
    }

    public void uniform1i(int location, int v) {
        glProgramUniform1i(handle, location, v);
    }

    public void uniform2i(int location, int v1, int v2) {
        glProgramUniform2i(handle, location, v1, v2);
    }

    public void uniform3i(int location, int v1, int v2, int v3) {
        glProgramUniform3i(handle, location, v1, v2, v3);
    }

    public void uniform4i(int location, int v1, int v2, int v3, int v4) {
        glProgramUniform4i(handle, location, v1, v2, v3, v4);
    }

    public void uniform1ui(int location, int v) {
        glProgramUniform1ui(handle, location, v);
    }

    public void uniform2ui(int location, int v1, int v2) {
        glProgramUniform2ui(handle, location, v1, v2);
    }

    public void uniform3ui(int location, int v1, int v2, int v3) {
        glProgramUniform3ui(handle, location, v1, v2, v3);
    }

    public void uniform4ui(int location, int v1, int v2, int v3, int v4) {
        glProgramUniform4ui(handle, location, v1, v2, v3, v4);
    }

    public void uniform1fv(int location, float[] values) {
        glProgramUniform1fv(handle, location, values);
    }

    public void uniform2fv(int location, float[] values) {
        glProgramUniform2fv(handle, location, values);
    }

    public void uniform3fv(int location, float[] values) {
        glProgramUniform3fv(handle, location, values);
    }

    public void uniform4fv(int location, float[] values) {
        glProgramUniform4fv(handle, location, values);
    }

    public void uniform1iv(int location, int[] v) {
        glProgramUniform1iv(handle, location, v);
    }

    public void uniform2iv(int location, int[] v) {
        glProgramUniform2iv(handle, location, v);
    }

    public void uniform3iv(int location, int[] v) {
        glProgramUniform3iv(handle, location, v);
    }

    public void uniform4fiv(int location, int[] v) {
        glProgramUniform4iv(handle, location, v);
    }

    public void uniform1uiv(int location, int[] v) {
        glProgramUniform1uiv(handle, location, v);
    }

    public void uniform2uiv(int location, int[] v) {
        glProgramUniform2uiv(handle, location, v);
    }

    public void uniform3uiv(int location, int[] v) {
        glProgramUniform3uiv(handle, location, v);
    }

    public void uniform4uiv(int location, int[] v) {
        glProgramUniform4uiv(handle, location, v);
    }

    public void uniformMatrix2fv(int location, boolean transpose, Matrix2f mat) {
        glProgramUniformMatrix2fv(handle, location, transpose, mat.get(new float[2*2]));
    }

    public void uniformMatrix3fv(int location, boolean transpose, Matrix3f mat) {
        glProgramUniformMatrix3fv(handle, location, transpose, mat.get(new float[3*3]));
    }

    public void uniformMatrix4fv(int location, boolean transpose, Matrix4f mat) {
        glProgramUniformMatrix4fv(handle, location, transpose, mat.get(new float[4*4]));
    }

    public void uniformMatrix4fv(int location, boolean transpose, Matrix4f mat, FloatBuffer buffer) {
        glProgramUniformMatrix4fv(handle, location, transpose, mat.get(buffer));
        buffer.clear();
    }

    public void uniformMatrix3x2fv(int location, boolean transpose, Matrix3x2f matrix) {
        glProgramUniformMatrix3x2fv(handle, location, transpose, matrix.get(new float[3*2]));
    }

    public void uniformMatrix4x3fv(int location, boolean transpose, Matrix4x3f matrix) {
        glProgramUniformMatrix4x3fv(handle, location, transpose, matrix.get(new float[4*3]));
    }

    public void label(String label) {
        glObjectLabel(GL_PROGRAM, handle, label);
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
