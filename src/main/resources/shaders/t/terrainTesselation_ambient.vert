#version 330

// Input attribures.
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 texCoord;
layout (location = 3) in vec3 tangent;
layout (location = 4) in vec3 bitanget;

// Oputput.
out vec2 texCoord0;
out vec3 normal0;
out vec3 worldPos0;

// Uniforms.
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

// Main method.
void main() {
    texCoord0 = texCoord;
    normal0 = (model * vec4(normal, 0)).xyz;
    worldPos0 = (model * vec4(position, 1)).xyz;
    gl_Position = projection * view * model * vec4(position, 1);
}