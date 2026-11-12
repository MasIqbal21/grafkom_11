package com.grafkom.visualrealism;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * ShadowMapFBO (Indikator 5: Shadows of Objects - Shadow Mapping 2-Pass)
 * ------------------------------------------------------------------------
 * Framebuffer Object khusus depth-only yang menampung Shadow Map (Depth Texture)
 * hasil render Pass 1 dari sudut pandang sumber cahaya.
 *
 * Pass 1 (Depth Pass) : bindForWriting() -> render scene dengan shadow_depth shader
 * Pass 2 (Lighting Pass): unbind() -> depthTextureID dipakai sebagai sampler2D "shadowMap"
 *                          pada scene.frag untuk membandingkan d_current vs d_shadow.
 */
public class ShadowMapFBO {

    private final int shadowWidth;
    private final int shadowHeight;
    private final int fboId;
    private final int depthTextureId;

    public ShadowMapFBO(int shadowWidth, int shadowHeight) {
        this.shadowWidth = shadowWidth;
        this.shadowHeight = shadowHeight;

        depthTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, shadowWidth, shadowHeight, 0,
                GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        float[] borderColor = { 1.0f, 1.0f, 1.0f, 1.0f }; // di luar frustum cahaya -> dianggap "tidak dalam bayangan"
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

        fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureId, 0);
        glDrawBuffer(GL_NONE); // tidak ada color attachment, ini pass depth-only
        glReadBuffer(GL_NONE);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("ShadowMapFBO tidak lengkap, status = 0x" + Integer.toHexString(status));
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /** Dipanggil sebelum Render Pass 1 (dari sudut pandang cahaya). */
    public void bindForWriting() {
        glViewport(0, 0, shadowWidth, shadowHeight);
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    /** Dipanggil setelah Pass 1 selesai, mengembalikan viewport & framebuffer layar untuk Pass 2. */
    public void unbind(int screenWidth, int screenHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, screenWidth, screenHeight);
    }

    public void bindDepthTexture(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, depthTextureId);
    }

    public int getDepthTextureId() { return depthTextureId; }

    public void destroy() {
        glDeleteFramebuffers(fboId);
        glDeleteTextures(depthTextureId);
    }
}
