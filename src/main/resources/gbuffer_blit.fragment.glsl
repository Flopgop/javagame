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
    int light_count;
    vec4 light_color;
} pbr_in;

uniform sampler2D gbuffer_position;
uniform sampler2D gbuffer_normal;
uniform sampler2D gbuffer_color;

layout(location = 0) out vec4 frag_color;

struct Light {
    vec3 position;
    float radius;

    vec3 color;
    float constant;

    float linear;
    float quadratic;
    vec2 _pad0;
};

layout(std430, binding = 2) readonly buffer LightBuffer {
    Light lights[];
};

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
    float specular_strength = 0.5;

    vec3 lighting = color * ambient;
    vec3 view_dir = normalize(cam_in.camera_pos - position);
    for (int i = 0; i < pbr_in.light_count; i++) {
        Light light = lights[i];
        float distance = length(light.position - position);
        if (distance > light.radius) continue;

        vec3 light_dir = normalize(light.position - position);
        vec3 diffuse = max(dot(normal, light_dir), 0.0) * color * light.color;
        vec3 halfway_dir = normalize(light_dir + view_dir);
        float spec = pow(max(dot(normal, halfway_dir), 0.0), 16.0);
        vec3 specular = light.color * spec * specular_strength;

        float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * distance * distance);
        diffuse *= attenuation;
        specular *= attenuation;
        lighting += diffuse + specular;
    }

    frag_color = vec4(tone_map_reinhard(lighting), 1.0);
}