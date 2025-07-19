#version 460 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 texcoord;

layout(std140, binding = 0) uniform CameraData {
    mat4 view;
    mat4 proj;
    vec3 camera_pos;
    float _pad0; // align to 16 bytes
} cam_in;

out FragmentInput {
    vec3 screen_pos;
    vec3 normal;
    vec2 texcoord;
} vs_out;

void main() {
    gl_Position = vec4(position.xz, 0.0, 1.0);
    vs_out.screen_pos = vec3(position.xz, 0.0);
    vs_out.normal = normal;
    vs_out.texcoord = texcoord;
}