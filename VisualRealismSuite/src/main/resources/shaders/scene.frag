// ==========================================================
// FILE: scene.frag (Fragment Shader utama - Pass 2 / Lighting Pass)
// Menggabungkan: Indikator 1 (Phong/Blinn-Phong), Indikator 4 (Texture Mapping),
//                Indikator 5 (Shadow Mapping + anti Shadow-Acne / Peter-Panning)
// ==========================================================
#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;

uniform sampler2D textureSampler;
uniform sampler2D shadowMap;
uniform bool useTexture;
uniform bool useBlinnPhong;   // true = Blinn-Phong (N.H), false = Phong klasik (R.V)  -> Indikator 1
uniform bool useShadows;

// ---- Indikator 5: Shadow Mapping dengan Slope-Scaled Bias (anti Shadow Acne & Peter Panning) ----
float computeShadow(vec4 fragPosLightSpace, vec3 normal, vec3 lightDir) {
    // Perspective divide -> NDC [-1,1], lalu remap ke [0,1] agar cocok dgn koordinat tekstur/depth
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;

    // Di luar frustum cahaya (far plane) -> anggap tidak ada bayangan
    if (projCoords.z > 1.0) return 0.0;

    float closestDepth = texture(shadowMap, projCoords.xy).r; // d_shadow
    float currentDepth = projCoords.z;                         // d_current

    // bias dinamis berdasarkan kemiringan normal terhadap arah cahaya (N . L)
    // mengatasi Shadow Acne tanpa bias terlalu besar yg menyebabkan Peter Panning
    float bias = max(0.05 * (1.0 - dot(normal, lightDir)), 0.005);

    // Percentage-Closer Filtering (PCF) 3x3 sederhana untuk mengurangi tepi bayangan yang kasar/aliasing
    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += (currentDepth - bias > pcfDepth) ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;
    return shadow; // 1.0 = sepenuhnya dalam bayangan, 0.0 = sepenuhnya terkena cahaya
}

void main() {
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    vec3 viewDir = normalize(viewPos - FragPos);

    // 1. Ambient
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse (Lambertian): I_diffuse ~ (N . L)
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular: Phong (R.V)^n  vs  Blinn-Phong (N.H)^n   -- Indikator 1
    float specularStrength = 0.8;
    float spec;
    if (useBlinnPhong) {
        vec3 halfwayDir = normalize(lightDir + viewDir);
        spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);
    } else {
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);
    }
    vec3 specular = specularStrength * spec * lightColor;

    // Shadow factor -- Indikator 5
    float shadow = useShadows ? computeShadow(FragPosLightSpace, norm, lightDir) : 0.0;

    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;

    // Ambient tetap tampil di area bayangan (tidak menjadi gelap total/hitam pekat)
    vec3 lighting = ambient + (1.0 - shadow) * (diffuse + specular);
    vec3 result = lighting * baseColor;

    FragColor = vec4(result, 1.0);
}
