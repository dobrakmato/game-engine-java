#version 330

// Input attribures.
layout (location = 0) in vec3 position;
layout (location = 1) in vec2 texCoord;

out vec2 texCoord0;

// Uniforms.
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

// Main method.
void main() {
    // Post effect are full-screen quads, so we
    // don't multiply position by any matrix.
    texCoord0 = texCoord;
    gl_Position = vec4(position, 1);
}