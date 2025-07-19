#version 460 core

layout (location = 0) in vec2 vertex_position;
layout (location = 1) in vec2 vertex_texcoord;
layout (location = 2) in vec4 vertex_glyph_uv;
layout (location = 3) in vec2 vertex_glyph_position;
layout (location = 4) in vec2 vertex_glyph_size;

out FragmentInput {
    vec2 texcoord;
    vec4 glyph_uv;
} vs_out;

uniform mat4 projection;

void main() {
    gl_Position = projection * vec4(vertex_glyph_position.xy + vertex_position.xy * vertex_glyph_size.xy, 0.0, 1.0);
    vs_out.texcoord = vertex_texcoord.xy;
    vs_out.glyph_uv = vertex_glyph_uv;
}