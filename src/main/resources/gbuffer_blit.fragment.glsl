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
    float _pad0; // align to 32 bytes
} cam_in;

layout(std140, binding = 1) uniform PBRData {
    vec4 ambient;
    vec3 light_direction;
    float _pad0;
    vec4 light_color;
} pbr_in;

uniform sampler2D gbuffer_position;
uniform sampler2D gbuffer_normal;
uniform sampler2D gbuffer_color;

layout(location = 0) out vec4 frag_color;

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

void main() {
    vec3 color = texture(gbuffer_color, fs_in.texcoord).rgb;
    vec3 normal = normalize(texture(gbuffer_normal, fs_in.texcoord).xyz);
    vec3 position = texture(gbuffer_position, fs_in.texcoord).rgb;

    vec3 ambient = pbr_in.ambient.rgb * pbr_in.ambient.a;

    vec3 dir = normalize(pbr_in.light_direction);

    vec3 light_color = pbr_in.light_color.rgb * pbr_in.light_color.a;

    float diff = max(dot(normal, dir), 0.0);
    vec3 diffuse = diff * light_color;

    float specular_strength = 0.5;
    float spec = 0.0;
    if (diff > 0.0) {
        vec3 view_dir = normalize(cam_in.camera_pos - position);
        vec3 reflect_dir = reflect(-dir, normal);
        spec = pow(max(dot(view_dir, reflect_dir), 0.0), 32);
    }
    vec3 specular = specular_strength * spec * light_color;

    vec3 result = (ambient + diffuse + specular) * color;
    frag_color = vec4(tone_map_reinhard(result), 1.0);
}