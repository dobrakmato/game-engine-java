#version 330

// Input attribures.
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 texCoord;
layout (location = 3) in vec3 tangent;

/*  todo:
Generate derivable vertex attributes inside the vertex program instead of storing
them inside of the input vertex format. For example, there is often no need to store
the tangent, binormal, and normal, since given any two, the third can be derived using
a cross-product in your vertex program.
*/

// Oputput.
out vec2 texCoord0;
out vec3 normal0;
out vec3 tangent0;
out vec3 worldPos0;

out vec3 N;
out vec3 E;

out mat3 TBN0;

// Uniforms.
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

uniform vec3 cameraPos;

// Main method.
void main() {
    // Normal mapping & Light
    vec4 worldSpacePosition = model * vec4(position, 1);
    gl_Position = projection * view * worldSpacePosition;

    normal0 = normalize((model * vec4(normal, 0)).xyz);
    vec3 tangent0 = normalize((model * vec4(tangent,  0)).xyz);
    vec3 bitangent0 = normalize((model * vec4(cross(normal, tangent), 0)).xyz);

    TBN0 = mat3(tangent0, bitangent0, normal0);
    mat3 invTBN = transpose(TBN0);

    N = normal;
    E = (worldSpacePosition.xyz - cameraPos);

    N = invTBN * N;
    E = invTBN * E;

    texCoord0 = texCoord;
    worldPos0 = worldSpacePosition.xyz;
}