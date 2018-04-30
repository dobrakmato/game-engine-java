#version 330

// Input attribures.
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 texCoord;
layout (location = 3) in vec3 tangent;
layout (location = 4) in vec3 bitanget;

// Oputput.
out vec2 texCoord0;
smooth out vec3 normal0;
out vec3 worldPos0;
out vec4 shadowCoord0;
out mat3 TBN0;

// Uniforms.
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix;

// Main method.
void main() {
    // Normal mapping & Light
    normal0 = normalize((model * vec4(normal, 0)).xyz);
    vec3 tangent0 = normalize((model * vec4(tangent,  0)).xyz);
    vec3 bitangent0 = normalize((model * vec4(bitanget, 0)).xyz);
    TBN0 = mat3(tangent0, bitangent0, normal0);

    // Texturing.
    texCoord0 = texCoord;


    worldPos0 = (model * vec4(position, 1)).xyz;
    gl_Position = projection * view * model * vec4(position, 1);

    // Shadow.
    shadowCoord0 = (lightSpaceMatrix * model * vec4(position, 1));
}