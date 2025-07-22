#version 460 core

in FragmentInput {
    vec2 texcoord;
} fs_in;

layout(location = 0) out vec4 color;

uniform sampler2D tex;
uniform vec3 tint;

void main()
{
    vec2 uv = fs_in.texcoord;
    vec4 sampled = texture(tex, uv);
    color = sampled * vec4(tint, 1.0);
}