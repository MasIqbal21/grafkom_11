// ==========================================================
// FILE: shadow_depth.frag (Render Pass 1 - Depth Pass)
// Tidak menulis warna apa pun; framebuffer ini depth-only (lihat ShadowMapFBO:
// glDrawBuffer(GL_NONE)). gl_FragDepth otomatis terisi oleh pipeline rasterisasi.
// ==========================================================
#version 330 core

void main() {
    // Sengaja kosong: hanya depth buffer yang relevan pada pass ini.
}
