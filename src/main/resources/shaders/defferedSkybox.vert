#version 330

// Input attribures.
layout (location = 0) in vec3 position;
layout (location = 1) in vec2 texCoord;

out vec2 texCoord0;
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
    texCoord0 = texCoord;

    // Directional light are full-screen quads, so we
    // don't multiply position by any matrix.
    gl_Position = vec4(position, 1);
}