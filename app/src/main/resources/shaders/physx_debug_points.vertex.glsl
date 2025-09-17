#version 460 core

layout(location = 0) in vec3 vertex_position;
layout(location = 1) in vec3 vertex_color;

layout(std140, binding = 0) uniform CameraData {
    mat4 view;
    mat4 proj;
    vec3 camera_pos;
    float _pad0; // always 0 for this shader
} cam_in;

out vec3 fragment_color;

void main() {
    gl_PointSize = 6.0;
    gl_Position = cam_in.proj * cam_in.view * vec4(vertex_position, 1.0);
    fragment_color = vertex_color;
}