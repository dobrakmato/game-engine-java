#version 330

in vec3 color0;
in vec3 normal0;
in vec3 worldPos0;

// Shader unfiroms
struct DirectionalLight {
	vec3 color;
    float intensity;
	vec3 direction;
} light;

uniform float specularIntensity;
uniform float specularPower;

uniform vec3 eyePos;

// TODO: Remove.
uniform vec3 sunColor;
uniform float sunIntensity = 1;
uniform vec3 sunDirection;
uniform vec3 color;

vec4 calcLight(DirectionalLight l, vec3 normal, vec3 worldPos) {
	float diffuseFactor = dot(normal, l.direction);

    vec4 ambientColor = vec4(0.1, 0.1, 0.1, 1);
	vec4 diffuseColor = vec4(0, 0, 0, 1);
	vec4 specularColor = vec4(0, 0, 0, 1);

	if(diffuseFactor > 0) {
		diffuseColor = vec4(l.color, 1.0) * l.intensity * diffuseFactor;

		vec3 directionToEye = normalize(eyePos - worldPos);
		vec3 reflectDirection = normalize(reflect(-l.direction, normal));

		float specularFactor = dot(directionToEye, reflectDirection);
		specularFactor = pow(specularFactor, specularPower);
		if(specularFactor > 0) {
			specularColor = vec4(l.color, 1) * specularIntensity * specularFactor;
		}
	}

	return ambientColor + diffuseColor + specularColor;
}

void main() {
    light.intensity = 1;
    light.color = sunColor;
    light.direction = sunDirection;
	gl_FragColor = vec4(color, 1.0) * calcLight(light, normalize(normal0), worldPos0);
}