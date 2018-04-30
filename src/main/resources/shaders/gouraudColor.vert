#version 330

// Input attribures.
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec3 color;

// Oputput.
out vec3 color0;

// Material uniforms.
uniform float specularIntensity = 0.2f;
uniform float specularPower = 4;

// Uniforms.
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

uniform vec3 eyePos;

// Unforms - Sun light source
uniform vec3 sunColor;
uniform float sunIntensity = 1;
uniform vec3 sunDirection;

vec4 calcLight(vec3 lightColor, vec3 direction, vec3 normal, vec3 worldPos) {
	float diffuseFactor = dot(normal, -direction);

    vec4 ambientColor = vec4(0.1, 0.1, 0.1, 1);
	vec4 diffuseColor = vec4(0, 0, 0, 1);
	vec4 specularColor = vec4(0, 0, 0, 1);

    if(diffuseFactor > 0) {
    	diffuseColor = vec4(lightColor, 1.0) * diffuseFactor;

		vec3 directionToEye = normalize(eyePos - worldPos);
		vec3 reflectDirection = normalize(reflect(direction, normal));

		float specularFactor = dot(directionToEye, reflectDirection);
		specularFactor = pow(specularFactor, specularPower);
		if(specularFactor > 0) {
			specularColor = vec4(lightColor, 1) * specularIntensity * specularFactor;
		}
    }
    return ambientColor + diffuseColor + specularColor;
}

vec4 calcDirectionalLight(vec3 lightColor, vec3 direction, vec3 normal, vec3 worldPos) {
	return calcLight(lightColor, -direction, normal, worldPos);
}


// Main method.
void main() {
    vec4 worldPosition = model * vec4(position, 1);
    vec4 positionRelativeToCamera = view * worldPosition;
    vec3 normal = normalize((model * vec4(normal, 0.0)).xyz);

    color0 = color * calcDirectionalLight(sunColor, sunDirection, normal, worldPosition.xyz).xyz;
    gl_Position =  projection * positionRelativeToCamera;
}