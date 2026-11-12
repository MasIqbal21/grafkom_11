package com.grafkom.visualrealism;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Mesh
 * ----
 * Wadah OOP untuk satu buah geometri GPU (VAO + VBO + opsional EBO).
 * Layout vertex (interleaved): [ posisi(x,y,z) | normal(x,y,z) | uv(u,v) ] -> stride 8 float.
 *
 * Satu objek 3D disimpan dalam DUA bentuk Mesh berbeda (lihat MeshFactory):
 *  - versi SMOOTH  : vertex ter-index, normal = rata-rata normal face yang terhubung (Indikator 2).
 *  - versi FLAT    : vertex tidak ter-index (tiap segitiga punya 3 vertex sendiri),
 *                    ketiganya memakai satu normal FACE yang sama -> hasil "facet shading".
 * Toggle Flat/Smooth pada aplikasi cukup berganti Mesh mana yang di-bind & digambar.
 */
public class Mesh {

    private static final int STRIDE_FLOATS = 8; // 3 pos + 3 normal + 2 uv

    private final int vao;
    private final int vbo;
    private final int ebo;
    private final int vertexCount;
    private final int indexCount;
    private final boolean indexed;

    /** Membuat mesh ter-index (dipakai untuk smooth shading). */
    public Mesh(float[] interleavedVertices, int[] indices) {
        this.indexed = true;
        this.vertexCount = interleavedVertices.length / STRIDE_FLOATS;
        this.indexCount = indices.length;

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vBuf = org.lwjgl.system.MemoryUtil.memAllocFloat(interleavedVertices.length);
        vBuf.put(interleavedVertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vBuf, GL_STATIC_DRAW);
        org.lwjgl.system.MemoryUtil.memFree(vBuf);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer iBuf = org.lwjgl.system.MemoryUtil.memAllocInt(indices.length);
        iBuf.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, iBuf, GL_STATIC_DRAW);
        org.lwjgl.system.MemoryUtil.memFree(iBuf);

        setupAttributes();
        glBindVertexArray(0);
    }

    /** Membuat mesh tanpa index (dipakai untuk flat shading / vertex ter-duplikasi). */
    public Mesh(float[] interleavedVertices) {
        this.indexed = false;
        this.vertexCount = interleavedVertices.length / STRIDE_FLOATS;
        this.indexCount = 0;

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vBuf = org.lwjgl.system.MemoryUtil.memAllocFloat(interleavedVertices.length);
        vBuf.put(interleavedVertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vBuf, GL_STATIC_DRAW);
        org.lwjgl.system.MemoryUtil.memFree(vBuf);

        this.ebo = 0;
        setupAttributes();
        glBindVertexArray(0);
    }

    private void setupAttributes() {
        int strideBytes = STRIDE_FLOATS * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, strideBytes, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, strideBytes, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, strideBytes, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
    }

    public void draw() {
        glBindVertexArray(vao);
        if (indexed) {
            glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, NULL);
        } else {
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        }
        glBindVertexArray(0);
    }

    public void destroy() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        if (ebo != 0) glDeleteBuffers(ebo);
    }
}
