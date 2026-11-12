package com.grafkom.visualrealism;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

/**
 * Texture2D (Indikator 4: Texture Mapping to Faces)
 * --------------------------------------------------
 * Memuat gambar PNG/JPG ke GPU memory memakai STBImage, lalu menyediakan
 * mode filtering yang bisa diganti saat runtime (tombol 'T'):
 *   - NEAREST : GL_NEAREST / GL_NEAREST             (blocky, Nearest Neighbor)
 *   - BILINEAR: GL_LINEAR_MIPMAP_LINEAR / GL_LINEAR (halus + mipmap, mengeliminasi aliasing)
 */
public class Texture2D {

    public enum FilterMode { NEAREST, BILINEAR_MIPMAPPED }

    private final int textureId;
    private FilterMode currentMode = FilterMode.BILINEAR_MIPMAPPED;

    /**
     * @param classpathResource path resource di dalam classpath, mis. "textures/checker.png"
     *                          (dibaca lewat memory buffer, bukan path file langsung, supaya
     *                          tetap berfungsi walau project sudah di-package jadi satu .jar).
     */
    public Texture2D(String classpathResource) {
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        applyFilterParams(currentMode);

        stbi_set_flip_vertically_on_load(true);
        ByteBuffer fileBuffer = readResourceToDirectBuffer(classpathResource);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer data = stbi_load_from_memory(fileBuffer, width, height, channels, 4);
            if (data != null) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0,
                        GL_RGBA, GL_UNSIGNED_BYTE, data);
                glGenerateMipmap(GL_TEXTURE_2D);
                stbi_image_free(data);
            } else {
                throw new RuntimeException("Gagal memuat tekstur: " + classpathResource
                        + " (" + stbi_failure_reason() + ")");
            }
        } finally {
            MemoryUtil.memFree(fileBuffer);
        }
    }

    private static ByteBuffer readResourceToDirectBuffer(String resourcePath) {
        try (InputStream in = Texture2D.class.getResourceAsStream("/" + resourcePath)) {
            if (in == null) {
                throw new IOException("Resource tekstur tidak ditemukan: " + resourcePath);
            }
            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca resource tekstur: " + resourcePath, e);
        }
    }

    /** Dipanggil saat tombol 'T' ditekan untuk membandingkan Nearest vs Bilinear+Mipmap (Rubrik Indikator 4). */
    public void setFilterMode(FilterMode mode) {
        this.currentMode = mode;
        glBindTexture(GL_TEXTURE_2D, textureId);
        applyFilterParams(mode);
    }

    private void applyFilterParams(FilterMode mode) {
        if (mode == FilterMode.NEAREST) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }
    }

    public FilterMode getCurrentMode() { return currentMode; }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void destroy() {
        glDeleteTextures(textureId);
    }
}
