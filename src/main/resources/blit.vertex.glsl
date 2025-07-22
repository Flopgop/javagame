#version 460 core

layout (location = 0) in vec2 vertex_position;
layout (location = 1) in vec2 vertex_texcoord;
layout (location = 2) in vec4 vertex_atlas_uv;
layout (location = 3) in vec2 vertex_instance_position;
layout (location = 4) in vec2 vertex_instance_size;

out FragmentInput {
    vec2 texcoord;
} vs_out;

uniform mat4 projection;

void main() {
    vec2 worldPos = vertex_instance_position.xy + vertex_position.xy * vertex_instance_size.xy;
    gl_Position = projection * vec4(worldPos, 0.0, 1.0);
    vs_out.texcoord = vertex_atlas_uv.xy + vertex_texcoord * vertex_atlas_uv.zw;
}