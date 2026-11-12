package com.grafkom.visualrealism;

import static org.lwjgl.opengl.GL11.*;

/**
 * HSRManager (Indikator 3: Hidden Surface Removal)
 * --------------------------------------------------
 * Mengonfigurasi dua teknik HSR dari modul teori:
 *   1. Z-Buffer / Depth Test   (tombol 'Z') -> glEnable(GL_DEPTH_TEST), GL_LESS
 *   2. Back-Face Culling       (tombol 'C') -> glEnable(GL_CULL_FACE), GL_BACK, CCW front face
 *
 * Kelas ini menyimpan status aktif saat ini agar bisa ditampilkan di title bar window
 * dan dipakai pada laporan praktikum (poin d: screenshot HSR On/Off).
 */
public class HSRManager {

    private boolean depthTestEnabled = true;
    private boolean backFaceCullingEnabled = true;

    public void setupDepthTest(boolean enable) {
        this.depthTestEnabled = enable;
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS); // Pixel lebih dekat ke kamera lolos uji depth
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }

    public void setupBackFaceCulling(boolean enable) {
        this.backFaceCullingEnabled = enable;
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);     // Buang polygon yang menghadap belakang kamera
            glFrontFace(GL_CCW);     // Counter-Clockwise dianggap tampak depan
        } else {
            glDisable(GL_CULL_FACE);
        }
    }

    public void toggleDepthTest() { setupDepthTest(!depthTestEnabled); }
    public void toggleBackFaceCulling() { setupBackFaceCulling(!backFaceCullingEnabled); }

    public boolean isDepthTestEnabled() { return depthTestEnabled; }
    public boolean isBackFaceCullingEnabled() { return backFaceCullingEnabled; }
}
