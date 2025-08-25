#version 460 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 texcoord;

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
    vec3 normal;
    vec2 texcoord;
} vs_out;

void main() {
    vec4 world_pos = obj_in.model * vec4(position, 1.0);
    vs_out.world_pos = world_pos.xyz;
    vs_out.normal = mat3(obj_in.normal) * normal;
    vs_out.texcoord = texcoord;

    gl_Position = cam_in.proj * cam_in.view * world_pos;
}