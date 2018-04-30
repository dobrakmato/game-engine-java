#version 330

in vec2 texCoord0;

uniform sampler2D texMap;

void main() {
	gl_FragColor = texture(texMap, texCoord0.yx);
}
