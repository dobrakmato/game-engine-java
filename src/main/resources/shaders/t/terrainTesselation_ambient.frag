#version 330

// Input
in vec2 texCoord0;
in vec3 normal0;
in vec3 worldPos0;

uniform sampler2D diffuseMap;       // texture 0

uniform vec3 cameraPos;

vec4 light_ambient() {
    vec4 ambientColor = vec4(0.16, 0.16, 0.16, 1); // lookup from indirect lighting cache
	return ambientColor;
}

void main() {
    vec4 ambient = texture(diffuseMap, texCoord0);
    vec4 light = light_ambient();
	gl_FragColor = ambient * light;
}