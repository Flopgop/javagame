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
    float _pad0;
} cam_in;

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

vec3 tonemap(int mode, vec3 color) {
    switch (mode) {
        case 0: return tone_map_reinhard(color);
        case 1: return tone_map_aces(color);
        case 2: return tone_map_uncharted2(color);
        default: return color;
    }
}

uniform int tonemap_mode; // passed into mode for tonemap
uniform vec2 screen_size; // width,height
uniform float z_near; // camera near
uniform float z_far; // camera far

uniform sampler2D img_texture; // fully rendered screen in HDR, not tonemapped.
uniform sampler2D depth_texture; // depth, not linear

uniform sampler2D gbuffer_position; // x,y,z in worldspace - camera_pos
uniform sampler2D gbuffer_normal; // normal in worldspace
uniform sampler2D gbuffer_material; // r = roughness, g = metallic

uniform sampler2D blue_noise;

layout(location = 0) out vec4 frag_color;

float linearize_depth(float depth)
{
    float z = depth * 2.0 - 1.0; // back to NDC
    return (2.0 * z_near * z_far) / (z_far + z_near - z * (z_far - z_near));
}

void main() {
    float depth = texture(depth_texture, fs_in.texcoord).r;
    vec3 color = texture(img_texture, fs_in.texcoord).rgb;

    frag_color = vec4(tonemap(tonemap_mode, color * contact_shadow), 1.0);
}