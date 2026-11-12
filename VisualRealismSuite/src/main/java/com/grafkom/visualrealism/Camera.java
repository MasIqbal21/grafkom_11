package com.grafkom.visualrealism;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Camera
 * ------
 * Kamera "fly" sederhana (WASD + mouse look + Q/E naik-turun) menggunakan JOML.
 * Mewarisi peran kamera dari project 'ThreeDViewing' (Minggu ke-10): menghasilkan
 * View Matrix & Projection Matrix untuk dikirim sebagai uniform ke GLSL shader.
 */
public class Camera {

    private final Vector3f position = new Vector3f(0f, 1.8f, 6f);
    private float yaw = -90f;   // derajat, menghadap -Z
    private float pitch = -10f;

    private final Vector3f front = new Vector3f(0, 0, -1);
    private final Vector3f up = new Vector3f(0, 1, 0);
    private final Vector3f right = new Vector3f(1, 0, 0);
    private final Vector3f worldUp = new Vector3f(0, 1, 0);

    private float moveSpeed = 4.0f;
    private final float mouseSensitivity = 0.12f;

    private float fovDegrees = 60f;
    private float nearPlane = 0.1f;
    private float farPlane = 100f;

    public Camera() {
        updateVectors();
    }

    public void processInput(Window window, float deltaTime) {
        float velocity = moveSpeed * deltaTime;
        if (window.isKeyDown(GLFW_KEY_LEFT_SHIFT)) velocity *= 2.5f;

        if (window.isKeyDown(GLFW_KEY_W)) position.fma(velocity, front);
        if (window.isKeyDown(GLFW_KEY_S)) position.fma(-velocity, front);
        if (window.isKeyDown(GLFW_KEY_A)) position.fma(-velocity, right);
        if (window.isKeyDown(GLFW_KEY_D)) position.fma(velocity, right);
        if (window.isKeyDown(GLFW_KEY_Q)) position.fma(-velocity, worldUp);
        if (window.isKeyDown(GLFW_KEY_E)) position.fma(velocity, worldUp);

        yaw += (float) (window.getMouseDeltaX() * mouseSensitivity);
        pitch += (float) (window.getMouseDeltaY() * mouseSensitivity);
        pitch = Math.max(-89f, Math.min(89f, pitch));

        updateVectors();
    }

    private void updateVectors() {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        Vector3f f = new Vector3f(
                (float) (Math.cos(yawRad) * Math.cos(pitchRad)),
                (float) Math.sin(pitchRad),
                (float) (Math.sin(yawRad) * Math.cos(pitchRad))
        ).normalize();
        front.set(f);
        right.set(front).cross(worldUp, right).normalize();
        up.set(right).cross(front, up).normalize();
    }

    public Matrix4f getViewMatrix(Matrix4f dest) {
        Vector3f center = new Vector3f(position).add(front);
        return dest.identity().lookAt(position, center, up);
    }

    public Matrix4f getProjectionMatrix(Matrix4f dest, float aspectRatio) {
        return dest.identity().perspective((float) Math.toRadians(fovDegrees), aspectRatio, nearPlane, farPlane);
    }

    public Vector3f getPosition() { return position; }
}
