#version 410 core

#include "includes/shadows.glsl"
#include "includes/lights.glsl"
#include "includes/bump_mapping.glsl"

in _TOFS {
     vec2 texCoord;
     vec3 normal;
     vec3 worldPos;
} TOFS;

uniform sampler2D diffuseMap;       // texture 0
uniform sampler2D cliffDiffuse;       // texture 7

uniform vec3 sunColor;
uniform vec3 sunDirection;

uniform vec3 cameraPos;

float lambert_diffuse(vec3 L, vec3 N) {
    return max(0.0, dot(L, N));
}

vec4 light_directional(vec3 color, vec3 direction, vec3 normal, vec3 worldPos) {
    vec4 lambert = vec4(color, 1.0) * lambert_diffuse(direction, normal);

    float visibility = 1.0;
    //if (castingShadows) {
    //    visibility = min(1.0, shadow_directional(shadowMap, shadowCoord0) + 0.33);
	//}
	return visibility * (lambert * visibility);
}

void main() {
    vec4 grass_d = texture(diffuseMap, TOFS.texCoord);
    vec4 cliff_d = texture(cliffDiffuse, TOFS.texCoord);
    vec4 diffuse = mix(grass_d, cliff_d, 0.001);
    //vec3 normal = normalmap_normal(TBN0, normalMap, TOFS.texCoord);
    vec4 light = light_directional(sunColor, sunDirection, TOFS.normal, TOFS.worldPos);
	gl_FragColor = diffuse * (light + vec4(0.1, 0.1, 0.1, 1));
    //gl_FragColor += vec4(TOFS.normal, 1);
}