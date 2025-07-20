#version 460 core

in FragmentInput {
    vec3 world_pos;
    vec3 normal;
    vec2 texcoord;
} fs_in;

uniform sampler2D texture_diffuse;

layout(location = 0) out vec3 gbuffer_position;
layout(location = 1) out vec3 gbuffer_normal;
layout(location = 2) out vec3 gbuffer_color;

void main() {
    gbuffer_position = fs_in.world_pos;
    gbuffer_normal = fs_in.normal;
    gbuffer_color = texture(texture_diffuse, fs_in.texcoord).rgb;
}