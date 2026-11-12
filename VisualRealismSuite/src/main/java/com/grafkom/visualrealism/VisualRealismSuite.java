package com.grafkom.visualrealism;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * VisualRealismSuite
 * -------------------
 * Aplikasi utama Tugas Praktikum Minggu ke-11: "3D Visual Realism Suite".
 * Memperluas Java 3D Viewer (Minggu ke-10) dengan 5 fitur wajib:
 *
 *   [B] Shading Model Switcher   : Phong  <-> Blinn-Phong        (Indikator 1)
 *   [F] Flat / Smooth Toggle     : per-face normal <-> per-vertex averaged normal (Indikator 2)
 *   [Z] Z-Buffer Depth Test      : on/off                        (Indikator 3)
 *   [C] Back-Face Culling        : on/off                        (Indikator 3)
 *   [T] Texture Filtering        : Nearest <-> Bilinear+Mipmap    (Indikator 4)
 *   [L] Light orbit pause/resume : memudahkan screenshot bayangan (Indikator 5, selalu aktif)
 *   [TAB] lepas/kunci cursor mouse, [ESC] keluar, WASD+mouse = kamera bebas
 */
public class VisualRealismSuite {

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    private static final int SHADOW_MAP_SIZE = 2048;

    private final Window window = new Window(WINDOW_WIDTH, WINDOW_HEIGHT, "Grafika Komputer - 3D Visual Realism Suite");
    private final Camera camera = new Camera();
    private final HSRManager hsr = new HSRManager();

    private Shader sceneShader;
    private Shader shadowDepthShader;
    private ShadowMapFBO shadowMapFBO;

    private Mesh sphereSmooth;
    private Mesh sphereFlat;
    private Mesh ground;
    private Texture2D checkerTexture;

    // ---- State toggle fitur (Bagian III: Spesifikasi Tugas) ----
    private boolean useBlinnPhong = true;     // Indikator 1
    private boolean flatShading = false;      // Indikator 2 (default: smooth)
    private boolean useTexture = true;        // Indikator 4
    private boolean lightOrbiting = true;     // bantu Indikator 5

    private float lightOrbitAngle = 45f;
    private final Vector3f lightColor = new Vector3f(1.0f, 0.96f, 0.88f);
    private final Vector3f sphereColor = new Vector3f(0.75f, 0.1f, 0.15f);

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        window.init();

        sceneShader = new Shader("shaders/scene.vert", "shaders/scene.frag");
        shadowDepthShader = new Shader("shaders/shadow_depth.vert", "shaders/shadow_depth.frag");
        shadowMapFBO = new ShadowMapFBO(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);

        // Mesh bermateri: UV-Sphere radius 1.2, 28 sector x 18 stack (facet cukup terlihat saat Flat Shading)
        sphereSmooth = MeshFactory.createSmoothSphere(1.2f, 28, 18);
        sphereFlat = MeshFactory.createFlatSphere(1.2f, 28, 18);
        ground = MeshFactory.createGroundPlane(6f, 6f);

        checkerTexture = new Texture2D("textures/checker.png");

        hsr.setupDepthTest(true);
        hsr.setupBackFaceCulling(true);

        glClearColor(0.07f, 0.08f, 0.11f, 1.0f);
        updateWindowTitle();
    }

    private void loop() {
        double lastTime = glfwGetTime();

        while (!window.shouldClose()) {
            double now = glfwGetTime();
            float deltaTime = (float) (now - lastTime);
            lastTime = now;

            handleToggleInput();
            camera.processInput(window, deltaTime);
            if (lightOrbiting) {
                lightOrbitAngle += deltaTime * 25f; // derajat/detik
            }

            Vector3f lightPos = computeLightPosition();
            Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
            Matrix4f lightProjection = new Matrix4f().ortho(-8f, 8f, -8f, 8f, 1f, 20f);
            Matrix4f lightSpaceMatrix = new Matrix4f(lightProjection).mul(lightView);

            renderShadowPass(lightSpaceMatrix);
            renderScenePass(lightPos, lightSpaceMatrix);

            window.endFrame();
        }
    }

    private Vector3f computeLightPosition() {
        double rad = Math.toRadians(lightOrbitAngle);
        float radius = 6f;
        return new Vector3f((float) (Math.cos(rad) * radius), 6.5f, (float) (Math.sin(rad) * radius));
    }

    // ---------------------------------------------------------------
    // Render Pass 1: Depth Pass dari sudut pandang cahaya (Indikator 5)
    // ---------------------------------------------------------------
    private void renderShadowPass(Matrix4f lightSpaceMatrix) {
        shadowMapFBO.bindForWriting();
        // Depth-only: culling depan dibalik opsional untuk mengurangi Peter-Panning pada mesh solid;
        // di sini kita nonaktifkan culling supaya seluruh permukaan menyumbang depth.
        glDisable(GL_CULL_FACE);

        shadowDepthShader.bind();
        shadowDepthShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);

        shadowDepthShader.setMat4("model", new Matrix4f().translate(0, 1.4f, 0));
        currentSphereMesh().draw();

        shadowDepthShader.setMat4("model", new Matrix4f());
        ground.draw();

        if (hsr.isBackFaceCullingEnabled()) glEnable(GL_CULL_FACE);
        shadowMapFBO.unbind(window.getWidth(), window.getHeight());
    }

    // ---------------------------------------------------------------
    // Render Pass 2: Lighting Pass normal dari sudut pandang kamera
    // ---------------------------------------------------------------
    private void renderScenePass(Vector3f lightPos, Matrix4f lightSpaceMatrix) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Matrix4f view = camera.getViewMatrix(new Matrix4f());
        float aspect = (float) window.getWidth() / Math.max(1, window.getHeight());
        Matrix4f projection = camera.getProjectionMatrix(new Matrix4f(), aspect);

        sceneShader.bind();
        sceneShader.setMat4("view", view);
        sceneShader.setMat4("projection", projection);
        sceneShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);

        sceneShader.setVec3("lightPos", lightPos);
        sceneShader.setVec3("viewPos", camera.getPosition());
        sceneShader.setVec3("lightColor", lightColor);

        sceneShader.setBool("useBlinnPhong", useBlinnPhong);
        sceneShader.setBool("useShadows", true);

        sceneShader.setInt("textureSampler", 0);
        sceneShader.setInt("shadowMap", 1);
        checkerTexture.bind(0);
        shadowMapFBO.bindDepthTexture(1);

        // --- Sphere bermateri ---
        sceneShader.setBool("useTexture", useTexture);
        sceneShader.setVec3("objectColor", sphereColor);
        sceneShader.setMat4("model", new Matrix4f().translate(0, 1.4f, 0));
        currentSphereMesh().draw();

        // --- Ground plane (selalu bertekstur, menerima bayangan) ---
        sceneShader.setBool("useTexture", true);
        sceneShader.setMat4("model", new Matrix4f());
        ground.draw();
    }

    private Mesh currentSphereMesh() {
        return flatShading ? sphereFlat : sphereSmooth;
    }

    private void handleToggleInput() {
        if (window.isKeyJustPressed(GLFW_KEY_B)) {
            useBlinnPhong = !useBlinnPhong;
            updateWindowTitle();
        }
        if (window.isKeyJustPressed(GLFW_KEY_F)) {
            flatShading = !flatShading;
            updateWindowTitle();
        }
        if (window.isKeyJustPressed(GLFW_KEY_Z)) {
            hsr.toggleDepthTest();
            updateWindowTitle();
        }
        if (window.isKeyJustPressed(GLFW_KEY_C)) {
            hsr.toggleBackFaceCulling();
            updateWindowTitle();
        }
        if (window.isKeyJustPressed(GLFW_KEY_T)) {
            Texture2D.FilterMode next = checkerTexture.getCurrentMode() == Texture2D.FilterMode.NEAREST
                    ? Texture2D.FilterMode.BILINEAR_MIPMAPPED
                    : Texture2D.FilterMode.NEAREST;
            checkerTexture.setFilterMode(next);
            updateWindowTitle();
        }
        if (window.isKeyJustPressed(GLFW_KEY_L)) {
            lightOrbiting = !lightOrbiting;
            updateWindowTitle();
        }
    }

    private void updateWindowTitle() {
        String status = String.format(
                "Shading=%s | Faces=%s | ZBuffer=%s | Culling=%s | Filter=%s | LightOrbit=%s",
                useBlinnPhong ? "Blinn-Phong" : "Phong",
                flatShading ? "Flat" : "Smooth",
                hsr.isDepthTestEnabled() ? "ON" : "OFF",
                hsr.isBackFaceCullingEnabled() ? "ON" : "OFF",
                checkerTexture.getCurrentMode() == Texture2D.FilterMode.NEAREST ? "Nearest" : "Bilinear+Mipmap",
                lightOrbiting ? "ON" : "OFF"
        );
        window.setTitle(status);
    }

    private void cleanup() {
        sphereSmooth.destroy();
        sphereFlat.destroy();
        ground.destroy();
        checkerTexture.destroy();
        shadowMapFBO.destroy();
        sceneShader.destroy();
        shadowDepthShader.destroy();
        window.destroy();
    }
}
