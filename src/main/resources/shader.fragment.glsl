#version 460 core

in FragmentInput {
    vec3 world_pos;
    vec3 normal;
    vec2 texcoord;
} fs_in;

layout(std140, binding = 0) uniform CameraData {
    mat4 view;
    mat4 proj;
    vec3 camera_pos;
    float _pad0; // align to 32 bytes
} cam_in;

layout(std140, binding = 2) uniform PBRData {
    vec4 ambient;
    vec3 light_direction;
    float _pad0;
    vec4 light_color;
} pbr_in;

uniform sampler2D texture_diffuse;

layout(location = 0) out vec4 frag_color;

void main() {
    vec3 color = texture(texture_diffuse, fs_in.texcoord).rgb;

    vec3 ambient = pbr_in.ambient.rgb * pbr_in.ambient.a;

    vec3 dir = normalize(pbr_in.light_direction);
    vec3 normal = normalize(fs_in.normal);

    vec3 light_color = pbr_in.light_color.rgb * pbr_in.light_color.a;

    float diff = max(dot(normal, dir), 0.0);
    vec3 diffuse = diff * light_color;

    float specular_strength = 0.5;
    float spec = 0.0;
    if (diff > 0.0) {
        vec3 view_dir = normalize(cam_in.camera_pos - fs_in.world_pos);
        vec3 reflect_dir = reflect(-dir, normal);
        spec = pow(max(dot(view_dir, reflect_dir), 0.0), 32);
    }
    vec3 specular = specular_strength * spec * light_color;

    vec3 result = (ambient + diffuse + specular) * color;
    frag_color = vec4(result, 1.0);
}

