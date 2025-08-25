#version 460 core

in FragmentInput {
    vec3 screen_pos;
    vec3 normal;
    vec2 texcoord;
} fs_in;

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

uniform int tonemap_mode;
uniform sampler2D img_texture;
uniform sampler2D depth_texture;

layout(location = 0) out vec4 frag_color;

vec3 tonemap(int mode, vec3 color) {
    switch (mode) {
        case 0: return tone_map_reinhard(color);
        case 1: return tone_map_aces(color);
        case 2: return tone_map_uncharted2(color);
        default: return color;
    }
}

void main() {
    vec4 color = texture(img_texture, fs_in.texcoord);

    frag_color = vec4(tonemap(tonemap_mode, color.rgb), 1.0);
}