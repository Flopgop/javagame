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

uniform sampler2D shadow_blue_noise;
uniform sampler2DShadow shadow_depth;

uniform float z_near;
uniform float z_far;
uniform ivec3 grid_size;
uniform ivec2 screen_dimensions;

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

struct Cluster
{
    vec4 min_point;
    vec4 max_point;
    uint count;
    uint light_indices[128];
};

layout(std430, binding = 2) readonly buffer LightBuffer {
    Light lights[];
};

layout(std430, binding = 3) readonly buffer ClusterBuffer {
    Cluster clusters[];
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

const vec2 poissonDisk[32] = vec2[](
    vec2(-0.326212, -0.405810),
    vec2(-0.840144, -0.073580),
    vec2(-0.695914,  0.457137),
    vec2(-0.203345,  0.620716),
    vec2( 0.962340, -0.194983),
    vec2( 0.473434, -0.480026),
    vec2( 0.519456,  0.767022),
    vec2( 0.185461, -0.893124),
    vec2( 0.507431,  0.064425),
    vec2( 0.896420,  0.412458),
    vec2(-0.321940, -0.932615),
    vec2(-0.791559, -0.597710),
    vec2(-0.413511,  0.888938),
    vec2(-0.289492,  0.384101),
    vec2( 0.870127, -0.761654),
    vec2( 0.685859, -0.156046),
    vec2( 0.469193,  0.417303),
    vec2( 0.635334,  0.778078),
    vec2(-0.579036, -0.699969),
    vec2(-0.645918, -0.420190),
    vec2(-0.342578,  0.126562),
    vec2(-0.050064,  0.941504),
    vec2( 0.607958, -0.903289),
    vec2( 0.828232, -0.584902),
    vec2( 0.742644,  0.307427),
    vec2( 0.441680,  0.822458),
    vec2(-0.263845, -0.711328),
    vec2(-0.505343, -0.395172),
    vec2(-0.157252,  0.574845),
    vec2(-0.034527,  0.902841),
    vec2( 0.682561, -0.274911),
    vec2( 0.918452,  0.145421)
);

float shadow_factor(vec3 world_pos) {
    vec3 sc = shadow_coords(world_pos);

    if (sc.x < 0.0 || sc.x > 1.0 || sc.y < 0.0 || sc.y > 1.0)
        return 1.0;

    float current_depth = sc.z - 0.0005;

    float shadow = 0.0;
    float texelScale = 2.0;
    vec2 texelSize = 1.0 / textureSize(shadow_depth, 0);
    texelSize *= texelScale;

    float rand = texture(shadow_blue_noise, gl_FragCoord.xy / textureSize(shadow_blue_noise, 0)).r;
    float angle = rand * 6.2831853; // 0..2π
    mat2 rot = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));

    // Apply rotation to poisson samples
    for (int i = 0; i < 16; i++) {
        vec2 offset = rot * poissonDisk[i];
        shadow += texture(shadow_depth, vec3(sc.xy + offset * texelSize, current_depth));
    }
    shadow /= 16.0;

    return shadow;
}

vec4 calculate_sky(vec3 rayPos, vec3 rayDir) {
    // somehow calculate procedural sky color, given all the many parameters here
    float t = clamp(rayDir.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 horizonColor = vec3(0.6, 0.8, 1.0);
    vec3 zenithColor = vec3(0.1, 0.3, 0.6);
    vec3 skyColor = mix(horizonColor, zenithColor, t);

    vec3 sunDir = -normalize(pbr_in.light_direction);
    float sunAmount = max(dot(rayDir, sunDir), 0.0);
    skyColor += vec3(1.0, 0.95, 0.8) * pow(sunAmount, 32.0);

    return vec4(skyColor, 1.0);
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

    vec3 view_space_position = (cam_in.view * vec4(position, 1.0)).xyz;

    uint z_tile = uint((log(abs(view_space_position.z) / z_near) * grid_size.z) / log(z_far / z_near));
    vec2 tile_size = screen_dimensions / grid_size.xy;
    uvec3 tile = uvec3(gl_FragCoord.xy / tile_size, z_tile);
    uint tile_index = tile.x + (tile.y * grid_size.x) + (tile.z * grid_size.x * grid_size.y);

    uint light_count = clusters[tile_index].count;

    for (int i = 0; i < light_count; i++) {
        uint light_index = clusters[tile_index].light_indices[i];

        Light light = lights[light_index];
        float distance = length(light.position - position);

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

    vec3 R = reflect(-V, N);
    vec4 skyReflection = calculate_sky(position, R);
    lighting += skyReflection.rgb * kS;

    vec2 ndc = fs_in.texcoord * 2.0 - 1.0;
    vec4 clipPos = vec4(ndc, 1.0, 1.0);
    vec4 viewPos = inverse(cam_in.proj) * clipPos;
    viewPos.xyz /= viewPos.w;
    vec3 rayDir = normalize((inverse(cam_in.view) * vec4(normalize(viewPos.xyz), 0.0)).xyz);
    frag_color = mix(vec4(tone_map_reinhard(lighting), 1.0), calculate_sky(cam_in.camera_pos, rayDir), float(depth >= 1.0));
}