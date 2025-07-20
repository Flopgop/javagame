#version 460 core

in FragmentInput {
    vec3 world_pos;
    vec3 normal;
    vec2 texcoord;
} fs_in;

uniform vec4 color;

layout(location = 0) out vec4 frag_color;

void main() {
    frag_color = color;
}

