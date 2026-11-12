package com.grafkom.visualrealism;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Window
 * ------
 * Membungkus pembuatan GLFW window + OpenGL 3.3 Core context.
 * Menyediakan input polling sederhana (keyboard "just pressed" + mouse delta)
 * yang dipakai oleh VisualRealismSuite untuk toggle fitur (B/F/Z/C/T) dan kamera.
 */
public class Window {

    private final int width;
    private final int height;
    private final String baseTitle;
    private long handle;

    // Keyboard state untuk deteksi "baru saja ditekan" (edge-trigger),
    // supaya menahan tombol tidak membuat toggle berganti berkali-kali per detik.
    private final boolean[] keysDown = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] keysDownPrev = new boolean[GLFW_KEY_LAST + 1];

    private double lastMouseX, lastMouseY;
    private double mouseDeltaX, mouseDeltaY;
    private boolean firstMouse = true;
    private boolean cursorCaptured = true;

    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.baseTitle = title;
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Gagal menginisialisasi GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // wajib di macOS
        glfwWindowHint(GLFW_SAMPLES, 4); // MSAA 4x

        handle = glfwCreateWindow(width, height, baseTitle, NULL, NULL);
        if (handle == NULL) {
            throw new RuntimeException("Gagal membuat GLFW window");
        }

        // Pusatkan window di layar
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode != null) {
            glfwSetWindowPos(handle, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);
        }

        glfwSetKeyCallback(handle, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_UNKNOWN) return;
            if (action == GLFW_PRESS) keysDown[key] = true;
            else if (action == GLFW_RELEASE) keysDown[key] = false;

            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(win, true);
            }
            if (key == GLFW_KEY_TAB && action == GLFW_PRESS) {
                toggleCursorCapture();
            }
        });

        glfwSetFramebufferSizeCallback(handle, (win, w, h) -> glViewport(0, 0, Math.max(1, w), Math.max(1, h)));

        glfwSetCursorPosCallback(handle, (win, xpos, ypos) -> {
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }
            mouseDeltaX += xpos - lastMouseX;
            mouseDeltaY += lastMouseY - ypos; // dibalik: layar y ke bawah, kamera y ke atas
            lastMouseX = xpos;
            lastMouseY = ypos;
        });

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1); // vsync
        glfwShowWindow(handle);

        GL.createCapabilities();

        glEnable(GL_MULTISAMPLE);

        setCursorCaptured(true);
    }

    /** Harus dipanggil tiap frame SETELAH input diproses, untuk reset delta & state "just pressed". */
    public void endFrame() {
        mouseDeltaX = 0;
        mouseDeltaY = 0;
        System.arraycopy(keysDown, 0, keysDownPrev, 0, keysDown.length);
        glfwSwapBuffers(handle);
        glfwPollEvents();
    }

    public boolean isKeyDown(int key) {
        return keysDown[key];
    }

    /** True hanya pada frame pertama saat tombol ditekan (tidak berulang selama ditahan). */
    public boolean isKeyJustPressed(int key) {
        return keysDown[key] && !keysDownPrev[key];
    }

    public double getMouseDeltaX() { return mouseDeltaX; }
    public double getMouseDeltaY() { return mouseDeltaY; }

    public void toggleCursorCapture() {
        setCursorCaptured(!cursorCaptured);
    }

    private void setCursorCaptured(boolean captured) {
        this.cursorCaptured = captured;
        glfwSetInputMode(handle, GLFW_CURSOR, captured ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    public void setTitle(String suffix) {
        glfwSetWindowTitle(handle, baseTitle + "  |  " + suffix);
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public int getWidth() {
        int[] w = new int[1], h = new int[1];
        glfwGetFramebufferSize(handle, w, h);
        return w[0];
    }

    public int getHeight() {
        int[] w = new int[1], h = new int[1];
        glfwGetFramebufferSize(handle, w, h);
        return h[0];
    }

    public long getHandle() { return handle; }

    public void destroy() {
        glfwFreeCallbacks(handle);
        glfwDestroyWindow(handle);
        glfwTerminate();
        var cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}
