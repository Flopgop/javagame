#version 460 core

in FragmentInput {
    vec3 world_pos;
    vec3 normal;
    vec2 texcoord;
} fs_in;

uniform sampler2D texture_diffuse;
uniform sampler2D texture_roughness;
uniform sampler2D texture_metallic;

layout(location = 0) out vec3 gbuffer_position;
layout(location = 1) out vec3 gbuffer_normal;
layout(location = 2) out vec3 gbuffer_color;
layout(location = 3) out vec4 gbuffer_material;

void main() {
    gbuffer_position = fs_in.world_pos;
    gbuffer_normal = fs_in.normal;
    gbuffer_color = texture(texture_diffuse, fs_in.texcoord).rgb;
    // in a sane workflow, rgb are all equal. In GLTF they're packed into one texture with g = roughness and b = metallic, so that's why this is weird.
    float roughness = texture(texture_roughness, fs_in.texcoord).g;
    float metallic = texture(texture_metallic, fs_in.texcoord).b;
    gbuffer_material = vec4(roughness, metallic, 0.0, 0.0);
}