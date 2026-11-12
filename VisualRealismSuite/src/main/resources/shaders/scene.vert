// ==========================================================
// FILE: scene.vert (Vertex Shader utama - Pass 2 / Lighting Pass)
// ==========================================================
#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoords;

out vec3 FragPos;
out vec3 Normal;
out vec2 TexCoords;
out vec4 FragPosLightSpace; // posisi fragment dalam Light Space, untuk Shadow Mapping (Indikator 5)

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix; // = P_light * V_light (lihat ShadowMapFBO + render pass 1)

void main() {
    FragPos = vec3(model * vec4(aPos, 1.0));

    // Normal Matrix: transpose(inverse(model)) agar normal tetap benar walau model di-scale non-uniform
    Normal = mat3(transpose(inverse(model))) * aNormal;

    TexCoords = aTexCoords;

    // Pos_light = P_light * V_light * M * Pos_world
    FragPosLightSpace = lightSpaceMatrix * vec4(FragPos, 1.0);

    gl_Position = projection * view * vec4(FragPos, 1.0);
}
