#version 330 core

in vec3 v3Direction;
in vec3 v3Color;
in vec3 v3SecondaryColor;

uniform vec3 v3LightPos;
uniform float g;
uniform float g2;

void main() {
	float fCos = dot(v3LightPos, v3Direction) / length(v3Direction);
    float fMiePhase = 1.5 * ((1.0 - g2) / (2.0 + g2)) * (1.0 + fCos*fCos) / pow(1.0 + g2 - 2.0*g*fCos, 1.5);
    gl_FragColor.rgb = v3Color + fMiePhase * v3SecondaryColor;
    //gl_FragColor.a = gl_FragColor.b;
    //gl_FragColor.rgb += vec3(0, 0, 0);
}
