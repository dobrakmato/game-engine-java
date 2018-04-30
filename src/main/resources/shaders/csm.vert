#version 330

layout(location = 0) in vec3 position;

uniform mat4 model;
uniform mat4 lightSpaceMatrix0;
uniform mat4 lightSpaceMatrix1;
uniform mat4 lightSpaceMatrix2;
uniform mat4 lightSpaceMatrix3;

void main() {
    gl_Position = lightSpaceMatrix0 * model * vec4(position, 1);
}
