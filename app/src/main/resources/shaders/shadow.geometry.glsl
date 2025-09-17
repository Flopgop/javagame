#version 460 core

#define NUM_CASCADES 4

layout(triangles, invocations = NUM_CASCADES) in;
layout(triangle_strip, max_vertices=3) out;

uniform mat4 cascade_matrices[NUM_CASCADES];

void main() {
    for (int i = 0; i < 3; i++) {
        gl_Position = cascade_matrices[gl_InvocationID] * gl_in[i].gl_Position;
        gl_Layer = gl_InvocationID;
        EmitVertex();
    }
    EndPrimitive();
}
