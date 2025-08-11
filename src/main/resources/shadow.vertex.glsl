#version 460 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 texcoord;

uniform mat4 shadow_view_proj;

layout(std140, binding = 1) uniform ObjectData {
    mat4 model;
} obj_in;

void main() {
    vec4 world_pos = obj_in.model * vec4(position, 1.0);

    gl_Position = shadow_view_proj * world_pos;
}