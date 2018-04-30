#version 330 core

// Input attribures.
layout (location = 0) in vec3 position;

uniform mat4 wvp;

out vec3 direction;

void main(void)
{
    direction = normalize(position);
	gl_Position = wvp * vec4(position, 1);
}
