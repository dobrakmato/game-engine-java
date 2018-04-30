#version 330

#include "includes/shadows.glsl"
#include "includes/lights.glsl"
#include "includes/bump_mapping.glsl"

// Input
in vec2 texCoord0;
in vec3 normal0;
in vec3 worldPos0;
in vec4 shadowCoord0;
in mat3 TBN0;

uniform SpotLight light;

uniform float specularIntensity;
uniform float specularPower;
uniform bool castingShadows;

uniform sampler2D diffuseMap;       // texture 0
uniform sampler2D normalMap;        // texture 1
uniform sampler2DShadow shadowMap;  // texture 2

uniform vec3 cameraPos;

vec4 light_0(SpotLight light, vec3 direction, vec3 normal, vec3 worldPos) {
	float diffuseFactor = dot(normal, direction);

	vec4 diffuseColor = vec4(0, 0, 0, 1);
	vec4 specularColor = vec4(0, 0, 0, 1);

	if(diffuseFactor > 0) {
		diffuseColor = vec4(light.color, 1.0) * light.intensity * diffuseFactor;

        if(specularIntensity > 0) {
		    vec3 directionToEye = normalize(cameraPos - worldPos);
		    vec3 reflectDirection = normalize(reflect(-direction, normal));

		    float specularFactor = dot(directionToEye, reflectDirection);
		    specularFactor = pow(specularFactor, specularPower);
		    if(specularFactor > 0) {
		    	specularColor = vec4(light.color, 1) * specularIntensity * specularFactor;
		    }
		}
	}

    float visibility = 1.0;
    if (castingShadows) {
        visibility = min(1.0, shadow_directional(shadowMap, shadowCoord0) + 0.33);
	}
	return visibility * (diffuseColor + specularColor * visibility);
}

vec4 light_point(SpotLight light, vec3 lightDir, vec3 normal, vec3 worldPos) {
	float distanceToPoint = length(worldPos0 - light.position);

	if(distanceToPoint > light.range) {
		return vec4(0, 0, 0, 0);
	}

	vec4 color = light_0(light, -lightDir, normal, worldPos);

	float attenuation = light.atten.constant +
	                    light.atten.linear * distanceToPoint +
	                    light.atten.quadratic * distanceToPoint * distanceToPoint +
	                    0.0001;

	return color / attenuation;
}

vec4 light_spot(SpotLight light, vec3 normal, vec3 worldPos) {
    vec3 lightDir = normalize(worldPos0 - light.position);
    float spotFactor = dot(lightDir, light.direction);

    vec4 color = vec4(0, 0, 0, 1);

    if(spotFactor > light.cutoff) {
        vec4 pointLight = light_point(light, lightDir, normal, worldPos);
    	color = pointLight * (1 - (1 - spotFactor) / (1 - light.cutoff));
    }

    return color;
}

void main() {
    vec4 diffuse = texture(diffuseMap, texCoord0);
    vec3 normal = normalmap_normal(TBN0, normalMap, texCoord0);
    vec4 light = light_spot(light, normal, worldPos0);
	gl_FragColor = diffuse * light;
}