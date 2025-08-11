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

uniform mat4 shadow_view_proj;

uniform sampler2D gbuffer_position;
uniform sampler2D gbuffer_normal;
uniform sampler2D gbuffer_color;
uniform sampler2D gbuffer_material;
uniform sampler2D gbuffer_depth;
uniform sampler2D shadow_depth;

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

vec3 shadow_coords(vec3 world_pos) {
    vec4 shadow_pos = shadow_view_proj * vec4(world_pos, 1.0);
    shadow_pos.xyz /= shadow_pos.w;
    return shadow_pos.xyz * 0.5 + 0.5;
}

float shadow_factor(vec3 world_pos) {
    vec3 sc = shadow_coords(world_pos);

    if (sc.x < 0.0 || sc.x > 1.0 || sc.y < 0.0 || sc.y > 1.0)
        return 1.0;

    float current_depth = sc.z;

    float bias = 0.001;
    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadow_depth, 0);
    for(int x = -1; x <= 1; ++x)
    {
        for(int y = -1; y <= 1; ++y)
        {
            float pcfDepth = texture(shadow_depth, sc.xy + vec2(x, y) * texelSize).r;
            shadow += current_depth - bias > pcfDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;

    return 1.0 - shadow;
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

    vec3 sunColor = pbr_in.light_color.rgb * pbr_in.light_color.a;
    vec3 sunDir = normalize(pbr_in.light_direction);

    vec3 L = normalize(-sunDir);
    vec3 H = normalize(V + L);

    float D = distributionGGX(N, H, roughness);
    float G = geometrySmith(N, V, L, roughness);
    vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

    vec3 kS = F;
    vec3 kD = (1.0 - kS) * (1.0 - metallic);

    vec3 diffuse = kD * (color / PI);
    vec3 specular = (D * G * F) / (4.0 * max(dot(N,V), 0.0) * max(dot(N, L), 0.0) + 0.001);

    float NdotL = max(dot(N, L), 0.0);

    float shadow = shadow_factor(position);
    lighting += shadow * (diffuse + specular) * sunColor * NdotL;

    frag_color = mix(vec4(tone_map_reinhard(lighting), 1.0), vec4(0.0, 0.0, 0.0, 1.0), float(depth == 1.0));
}