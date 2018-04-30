#version 410 core

// Input attribures.
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 texCoord;
layout (location = 3) in vec3 tangent;
layout (location = 4) in vec3 bitanget;
layout (location = 5) in vec2 dispTexCoord;

// Uniforms.
uniform mat4 model;

out _TOCS {
    vec2 texCoord;
    vec2 dispTexCoord;
    vec3 normal;
    vec3 worldPos;
} TOCS;

// Main method.
void main() {
    TOCS.texCoord = texCoord;
    TOCS.dispTexCoord = dispTexCoord;
    TOCS.normal = (model * vec4(normal, 0)).xyz;
    TOCS.worldPos = (model * vec4(position, 1)).xyz;
    gl_Position = (model * vec4(position, 1));
}