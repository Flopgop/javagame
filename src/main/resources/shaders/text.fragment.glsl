#version 460 core

in FragmentInput {
    vec2 texcoord;
    vec4 glyph_uv;
} fs_in;

layout(location = 0) out vec4 color;

uniform sampler2D atlas;
uniform vec3 text_color;

void main()
{
    vec2 uv = fs_in.glyph_uv.xy + fs_in.texcoord * fs_in.glyph_uv.zw;
    vec4 sampled = vec4(1.0, 1.0, 1.0, texture(atlas, uv).r);
    color = vec4(text_color, 1.0) * sampled;
}