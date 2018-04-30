#version 330

// Shader features.
// #define FOG

// Input attribures.
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec3 color;

// Oputput.
out vec3 color0;
out float visibility0;

// Uniforms.
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

uniform vec3 eyePos;

// Uniforms - Fog
uniform float fogDensity = 0.0007;
uniform float fogGradient = 2;
uniform float fogHeight = 5;

const float specularIntensity = 0.2f;
const float specularPower = 4;

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
	gl_Position =  projection * positionRelativeToCamera;

    color0 = color * calcDirectionalLight(sunColor, sunDirection, normal, position).xyz;

    #ifdef FOG
	    float distance = length(positionRelativeToCamera.xyz);
	    visibility0 = exp(-pow((distance*fogDensity), fogGradient));
	    visibility0 = clamp(visibility0, 0.0, 1.0);
	#endif
}