#version 330

// Input attribures.
layout (location = 0) in vec3 position;

out mat4 invProj;
out mat4 invView;

// Uniforms.
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

// Main method.
void main() {
    invView = inverse(view);
    invProj = inverse(projection);

    gl_Position = projection * view * model * vec4(position, 1);
}