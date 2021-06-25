#version 330

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 texCoord;

uniform mat4 projection;
uniform mat4 view;

out mat4 invProj;
out mat4 invView;
out vec2 texCoord0;

void main() {
    invProj = inverse(projection);
    invView = inverse(view);
    texCoord0 = texCoord;
	gl_Position = vec4(position.x, position.y, 0.0, 1.0);
}
