#version 460 core

in FragmentInput {
    vec3 world_pos;
    vec2 texcoord;
    mat3 TBN;
} fs_in;

layout(std140, binding = 0) uniform CameraData {
    mat4 view;
    mat4 proj;
    vec3 camera_pos;
    float _pad0;
} cam_in;

uniform sampler2D texture_diffuse;
uniform sampler2D texture_roughness;
uniform sampler2D texture_metallic;
uniform sampler2D texture_normal;

layout(location = 0) out vec3 gbuffer_position;
layout(location = 1) out vec3 gbuffer_normal;
layout(location = 2) out vec3 gbuffer_color;
layout(location = 3) out vec4 gbuffer_material;

void main() {
    gbuffer_position = fs_in.world_pos - cam_in.camera_pos;

    vec3 normal = texture(texture_normal, fs_in.texcoord).rgb;
    normal = normalize(normal * 2.0 - 1.0);
    normal = normalize(fs_in.TBN * normal);

    gbuffer_normal = normal;
    gbuffer_color = texture(texture_diffuse, fs_in.texcoord).rgb;
    // in a sane workflow, rgb are all equal. In GLTF they're packed into one texture with g = roughness and b = metallic, so that's why this is weird.
    float roughness = texture(texture_roughness, fs_in.texcoord).g;
    float metallic = texture(texture_metallic, fs_in.texcoord).b;
    gbuffer_material = vec4(roughness, metallic, 0.0, 0.0);
}