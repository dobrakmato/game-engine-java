#version 330

// Input attribures.
layout (location = 0) in vec3 position;

// Uniforms.
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

// Main method.
void main() {

    // Directional light are full-screen quads, so we
    // don't multiply position by any matrix.
    gl_Position = vec4(position, 1);
}