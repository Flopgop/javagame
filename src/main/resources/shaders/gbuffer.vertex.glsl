#version 460 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texcoord;
layout(location = 2) in vec4 normal;
layout(location = 3) in vec4 tangent;

layout(std140, binding = 0) uniform CameraData {
    mat4 view;
    mat4 proj;
    vec3 camera_pos;
    float _pad0;
} cam_in;

layout(std140, binding = 1) uniform ObjectData {
    mat4 model;
    mat4 normal;
} obj_in;

out FragmentInput {
    vec3 world_pos;
    vec2 texcoord;
    mat3 TBN;
} vs_out;

void main() {
    vec4 world_pos = obj_in.model * vec4(position, 1.0);
    vs_out.world_pos = world_pos.xyz;
    vs_out.texcoord = texcoord;

    vec3 n = normalize(normal.xyz);
    vec3 t = normalize(tangent.xyz);
    vec3 b = normalize(cross(n, t) * (tangent.w * 2.0 - 1.0));
    mat3 tbn = mat3(t, b, n);

    vs_out.TBN = mat3(obj_in.model) * tbn;

    gl_Position = cam_in.proj * cam_in.view * world_pos;
}