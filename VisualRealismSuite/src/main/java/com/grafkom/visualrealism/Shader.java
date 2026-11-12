package com.grafkom.visualrealism;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

/**
 * Shader
 * ------
 * Utilitas OOP untuk compile + link GLSL Vertex/Fragment Shader (#version 330 core)
 * menjadi satu Shader Program, beserta pengaturan uniform (mat4, vec3, int, bool, float).
 */
public class Shader {

    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public Shader(String vertexResourcePath, String fragmentResourcePath) {
        int vertexId = compile(loadSource(vertexResourcePath), GL_VERTEX_SHADER, vertexResourcePath);
        int fragmentId = compile(loadSource(fragmentResourcePath), GL_FRAGMENT_SHADER, fragmentResourcePath);

        programId = glCreateProgram();
        glAttachShader(programId, vertexId);
        glAttachShader(programId, fragmentId);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programId, 2048);
            throw new RuntimeException("Gagal link shader program (" + vertexResourcePath + ", "
                    + fragmentResourcePath + "):\n" + log);
        }

        glDetachShader(programId, vertexId);
        glDetachShader(programId, fragmentId);
        glDeleteShader(vertexId);
        glDeleteShader(fragmentId);
    }

    private static String loadSource(String resourcePath) {
        try (InputStream in = Shader.class.getResourceAsStream("/" + resourcePath)) {
            if (in == null) {
                throw new IOException("Resource tidak ditemukan di classpath: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca shader: " + resourcePath, e);
        }
    }

    private static int compile(String source, int type, String name) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(id, 2048);
            throw new RuntimeException("Gagal compile shader [" + name + "]:\n" + log);
        }
        return id;
    }

    public void bind() { glUseProgram(programId); }
    public void unbind() { glUseProgram(0); }

    private int location(String name) {
        return uniformCache.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    public void setMat4(String name, Matrix4f matrix) {
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            glUniformMatrix4fv(location(name), false, matrix.get(stack.mallocFloat(16)));
        }
    }

    public void setVec3(String name, Vector3f v) {
        glUniform3f(location(name), v.x, v.y, v.z);
    }

    public void setVec3(String name, float x, float y, float z) {
        glUniform3f(location(name), x, y, z);
    }

    public void setFloat(String name, float value) {
        glUniform1f(location(name), value);
    }

    public void setInt(String name, int value) {
        glUniform1i(location(name), value);
    }

    public void setBool(String name, boolean value) {
        glUniform1i(location(name), value ? 1 : 0);
    }

    public void destroy() {
        glDeleteProgram(programId);
    }
}
