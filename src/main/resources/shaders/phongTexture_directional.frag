#version 330

#include "includes/shadows.glsl"
#include "includes/lights.glsl"
#include "includes/bump_mapping.glsl"

// Input
in vec2 texCoord0;
smooth in vec3 normal0;
in vec3 worldPos0;
in vec4 shadowCoord0;
in mat3 TBN0;

uniform DirectionalLight light;

uniform float specularIntensity;
uniform float specularPower;
uniform bool castingShadows;

uniform sampler2D diffuseMap;       // texture 0
uniform sampler2D normalMap;        // texture 1
uniform sampler2DShadow shadowMap;  // texture 2

uniform vec3 cameraPos;

float lambert_diffuse(vec3 L, vec3 N) {
    return max(0.0, dot(L, N));
}

vec4 light_directional(DirectionalLight light, vec3 normal, vec3 worldPos) {
    vec4 lambert = vec4(light.color, 1.0) * light.intensity * lambert_diffuse(light.direction, normal);
    vec4 specular = vec4(0, 0, 0, 1);

    if(specularIntensity > 0) {
        vec3 directionToEye = normalize(cameraPos - worldPos);
        vec3 reflectDirection = normalize(reflect(-light.direction, normal));

        float specularFactor = dot(directionToEye, reflectDirection);
        specularFactor = pow(specularFactor, specularPower);
        if(specularFactor > 0) {
        	specular = vec4(light.color, 1) * specularIntensity * specularFactor;
        }
    }

    float visibility = 1.0;
    if (castingShadows) {
        visibility = min(1.0, shadow_directional(shadowMap, shadowCoord0) + 0.33);
	}
	return visibility * (lambert + specular * visibility);
}

void main() {
    vec4 diffuse = texture(diffuseMap, texCoord0);
    vec3 normal = normalmap_normal(TBN0, normalMap, texCoord0);
    vec4 light = light_directional(light, normal, worldPos0);
	gl_FragColor = diffuse * light / 10000;
	gl_FragColor += vec4(normal, 1);
}