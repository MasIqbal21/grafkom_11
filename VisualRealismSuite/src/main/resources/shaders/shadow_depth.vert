// ==========================================================
// FILE: shadow_depth.vert (Render Pass 1 - Depth Pass dari sudut pandang cahaya)
// ==========================================================
#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;   // tidak dipakai di pass ini, tetap dideklarasikan agar
layout (location = 2) in vec2 aTexCoords; // layout VAO sama persis dengan scene.vert

uniform mat4 lightSpaceMatrix; // P_light * V_light
uniform mat4 model;

void main() {
    gl_Position = lightSpaceMatrix * model * vec4(aPos, 1.0);
}
