#version 330

// Input attribures.
layout (location = 0) in vec3 position;

// Oputput.
out vec3 normal0;
flat out vec4 finalLight0;

// Uniforms.
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

uniform vec3 eyePos;
uniform vec3 sunColor;
uniform vec3 sunDirection;

uniform float time;

const float specularIntensity = 0.66f;
const float specularPower = 8;

const float waveSpeed = 1 / 800.0f;

vec4 calcLight(vec3 lightColor, vec3 direction, vec3 normal, vec3 worldPos) {
	float diffuseFactor = dot(normal, -direction);

    vec4 ambientColor = vec4(0.4, 0.4, 0.4, 1);
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
    vec3 pos = position;

    float ax = (time * waveSpeed + pos.x);
    float az = (time * waveSpeed + pos.z);

    float sinx = sin(ax);
    float cosx = cos(ax);
    float cosz = cos(az);

    pos.y = (sinx + cosz);

    vec3 normal = vec3(cosx *0.5, clamp(abs(sinx), 0.5, 1.0), cosz*0.5);
    normal = normalize(normal);

    finalLight0 = calcDirectionalLight(sunColor, sunDirection, normal, pos);

	gl_Position = projection * view * model * vec4(pos, 1);
}