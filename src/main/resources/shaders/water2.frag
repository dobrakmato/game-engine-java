#version 330

in vec3 normal0;
flat in vec4 finalLight0;

uniform vec4 waterColor = vec4(24/255f, 128/255f, 192/255f, 1);

void main() {
	gl_FragColor = waterColor * finalLight0;
}