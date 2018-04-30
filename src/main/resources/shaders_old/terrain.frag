#version 330

// Shader features.
#define FOG

in vec3 color0;
in float visibility0;

uniform vec3 fogColor = vec3(54/255f, 140/255f, 255/255f);

void main() {
	gl_FragColor = vec4(color0.x, color0.y, color0.z, 1);

	#ifdef FOG
	    gl_FragColor = mix(vec4(fogColor, 1.0), gl_FragColor, visibility0);
	#endif
}