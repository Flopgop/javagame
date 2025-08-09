#version 460 core

in FragmentInput {
    vec3 screen_pos;
    vec3 normal;
    vec2 texcoord;
} fs_in;

layout(std140, binding = 0) uniform CameraData {
    mat4 view;
    mat4 proj;
    vec3 camera_pos;
    float _pad0; // always 0 for this shader
} cam_in;

layout(std140, binding = 1) uniform PBRData {
    vec4 ambient;
    vec4 light_color;
    vec3 light_direction;
    float light_count;
} pbr_in;


uniform sampler2D gbuffer_position;
uniform sampler2D gbuffer_normal;
uniform sampler2D gbuffer_color;
uniform sampler2D gbuffer_material;
uniform sampler2D gbuffer_depth;

layout(location = 0) out vec4 frag_color;

struct Light {
    vec3 position;
    float radius;

    vec3 color;
    float constant;

    float linear;
    float quadratic;
    vec2 _pad0;
};

layout(std430, binding = 2) readonly buffer LightBuffer {
    Light lights[];
};

vec3 tone_map_reinhard(vec3 color) {
    return color / (color + vec3(1.0));
}

vec3 tone_map_aces(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

vec3 tone_map_uncharted2(vec3 x) {
    const float A = 0.15;
    const float B = 0.50;
    const float C = 0.10;
    const float D = 0.20;
    const float E = 0.02;
    const float F = 0.30;
    const float W = 11.2; // white point

    vec3 curr = ((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F)) - E/F;
    vec3 white = ((vec3(W)*(A*vec3(W)+C*B)+D*E)/(vec3(W)*(A*vec3(W)+B)+D*F)) - E/F;
    return curr / white;
}

const float PI = 3.14159265359;

vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

float distributionGGX(vec3 N, vec3 H, float roughness) {
    float a      = roughness * roughness;
    float a2     = a * a;
    float NdotH  = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;

    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    return a2 / (PI * denom * denom);
}

float geometrySchlickGGX(float NdotV, float roughness) {
    float r = roughness + 1.0;
    float k = (r * r) / 8.0;

    return NdotV / (NdotV * (1.0 - k) + k);
}


float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx1 = geometrySchlickGGX(NdotV, roughness);
    float ggx2 = geometrySchlickGGX(NdotL, roughness);
    return ggx1 * ggx2;
}

void main() {
    vec3 color = texture(gbuffer_color, fs_in.texcoord).rgb;
    vec3 normal = normalize(texture(gbuffer_normal, fs_in.texcoord).xyz);
    vec3 position = texture(gbuffer_position, fs_in.texcoord).rgb;
    float depth = texture(gbuffer_depth, fs_in.texcoord).r;

    vec4 material = texture(gbuffer_material, fs_in.texcoord);
    float roughness = material.r;
    float metallic = material.g;

    vec3  F0 = mix(vec3(0.04), color, metallic);

    vec3 ambient = pbr_in.ambient.rgb * pbr_in.ambient.a;
    vec3 lighting = ambient * color;

    vec3 V = normalize(cam_in.camera_pos - position);
    vec3 N = normal;

    vec3 Ls = normalize(-pbr_in.light_direction);
    vec3 Hs = normalize(V + Ls);

    float D_sky = distributionGGX(N, Hs, roughness);
    float G_sky = geometrySmith(N, V, Ls, roughness);
    vec3  F_sky = fresnelSchlick(max(dot(Hs, V), 0.0), F0);

    vec3 kS_sky = F_sky;
    vec3 kD_sky = (1.0 - kS_sky) * (1.0 - metallic);

    vec3 diffuse_sky  = kD_sky * (color / PI);
    vec3 specular_sky = (D_sky * G_sky * F_sky) /
    (4.0 * max(dot(N, V), 0.0) * max(dot(N, Ls), 0.0) + 0.001);

    float NdotL_sky = max(dot(N, Ls), 0.0);
    vec3 radiance_sky = pbr_in.light_color.rgb;
    lighting += (diffuse_sky + specular_sky) * radiance_sky * NdotL_sky;

    for (int i = 0; i < int(pbr_in.light_count); i++) {
        Light light = lights[i];
        float distance = length(light.position - position);
        if (distance > light.radius) continue;

        vec3 L = normalize(light.position - position);
        vec3 H = normalize(V + L);

        float D = distributionGGX(N, H, roughness);
        float G = geometrySmith(N, V, L, roughness);
        vec3  F = fresnelSchlick(max(dot(H, V), 0.0), F0);

        vec3 kS = F;
        vec3 kD = (1.0 - kS) * (1.0 - metallic);

        vec3 diffuse  = kD * (color / PI);
        vec3 specular = (D * G * F) /
        (4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.001);

        float attenuation = 1.0 /
        (light.constant + light.linear * distance + light.quadratic * distance * distance);

        vec3 radiance = light.color * attenuation;
        float NdotL = max(dot(N, L), 0.0);

        lighting += (diffuse + specular) * radiance * NdotL;
    }

    frag_color = mix(vec4(tone_map_reinhard(lighting), 1.0), vec4(0.0, 0.0, 0.0, 1.0), float(depth == 1.0));
}