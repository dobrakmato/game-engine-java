#version 330

in vec3 normal0;
flat in vec4 dirLight0;

uniform vec4 waterColor = vec4(0, 0, 192/255f, 1);

void main() {
	gl_FragColor = vec4(normal0, 1); //waterColor * dirLight0;
}