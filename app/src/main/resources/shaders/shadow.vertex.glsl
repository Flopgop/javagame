#version 460 core

layout(location = 0) in vec3 position;

layout(std140, binding = 1) uniform ObjectData {
    mat4 model;
    mat4 normal;
} obj_in;

void main() {
    vec4 world_pos = obj_in.model * vec4(position, 1.0);

    gl_Position = world_pos;
}