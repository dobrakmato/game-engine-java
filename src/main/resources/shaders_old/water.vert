#version 330

// Input attribures.
layout (location = 0) in vec3 position;

// Oputput.
out vec3 normal0;
flat out vec4 dirLight0;

// Uniforms.
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

uniform float time;
uniform vec3 eyePos;

const float specularIntensity = 0.5f;
const float specularPower = 16;

const vec3 sunColor = vec3(1,1,0);
const vec3 sunDirection = normalize(vec3(0.5, -0.5, 0.5));

vec4 calcLight(vec3 lightColor, vec3 direction, vec3 normal, vec3 worldPos) {
	float diffuseFactor = dot(normal, -direction);
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

    	return diffuseColor + specularColor;
}

vec4 calcDirectionalLight(vec3 lightColor, vec3 direction, vec3 normal, vec3 worldPos) {
	return calcLight(lightColor, -direction, normal, worldPos);
}

float mod(float x, float y) {
  return x - y * floor(x/y);
}

const float wave_frequency = 1 / 100.0;
const float wave_speed = 1 / 5.0;
const float wave_amplitude = 30.0;

// Main method.
void main() {
    vec3 pos = position;
	//float speed = waveSpeed + (waveSpeed * sin( mod(pos.x * 7, 385) + mod(pos.z * 3, 437) / 1400) * 0.5);
	//
	//float xComp = sin(time / speed + sin(speed) + ((pos.x) / 60.0)) * waveHeight;
	//float zComp = cos(time / speed + cos(speed) + ((pos.z) / 60.0)) * waveHeight;
    //pos.y += xComp + zComp;
    //vec3 normal = normalize(vec3(0.05 + (xComp+3)/7, 0, 0.05 + (zComp +3)/7));

	float comp1 = sin((time * wave_speed + pos.x) * wave_frequency * 0.86352);
	float comp2 = cos((time * wave_speed + pos.z) * wave_frequency * 0.85798);
	float step0 = wave_amplitude *
	(
		comp1 +
		comp2
	);

	pos.y = step0;
	vec3 normal = normalize(vec3(step0 * 0.86352, 1, step0 * 0.85798)); //vec3(s, s/c, c);

    dirLight0 = calcDirectionalLight(sunColor, sunDirection, normal, pos);
    normal0 = normal;

	gl_Position = projection * view * model * vec4(pos, 1);
}