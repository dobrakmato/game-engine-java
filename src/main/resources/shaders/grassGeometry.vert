#version 330

// Input attribures.
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 texCoord;
layout (location = 3) in vec3 tangent;

layout (location = 5) in vec4 mat_0;
layout (location = 6) in vec4 mat_1;
layout (location = 7) in vec4 mat_2;
layout (location = 8) in vec4 mat_3;

layout (location = 9) in vec3 instanceColor;

/*  todo:
Generate derivable vertex attributes inside the vertex program instead of storing
them inside of the input vertex format. For example, there is often no need to store
the tangent, binormal, and normal, since given any two, the third can be derived using
a cross-product in your vertex program.
*/

// Oputput.
out vec2 texCoord0;
//out vec3 normal0;
out vec3 tangent0;
out vec3 worldPos0;

out mat3 TBN0;

out vec3 tintColor0;

// Uniforms.
uniform mat4 view;
uniform mat4 projection;

// Main method.
void main() {

    mat4 model = transpose(mat4(
        mat_0,
        mat_1,
        mat_2,
        mat_3
    ));

    vec4 worldSpacePosition = model * vec4(position, 1);

    gl_Position = projection * view * worldSpacePosition;

    vec3 normal0 = normalize((model * vec4(normal, 0)).xyz);
    vec3 tangent0 = normalize((model * vec4(tangent,  0)).xyz);
    vec3 bitangent0 = normalize((model * vec4(cross(normal, tangent), 0)).xyz);

    TBN0 = mat3(tangent0, bitangent0, normal0);

    texCoord0 = texCoord;
    tintColor0 = instanceColor;
    worldPos0 = worldSpacePosition.xyz;
}