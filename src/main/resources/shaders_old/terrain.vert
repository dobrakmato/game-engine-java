#version 330

// Input attribures.
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 color;
layout (location = 2) in vec3 normal;

// Oputput.
out vec3 color0;
out float visibility0;

// Uniforms.
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform float time;

// Uniforms - Fog
uniform float fogDensity = 0.00007;
uniform float fogGradient = 2;
uniform float fogHeight = 5;

// Unforms - Sun light source
uniform vec3 sunColor;
uniform float sunIntensity = 1;
uniform vec3 sunDirection;

vec4 calcLight(vec3 lightColor, vec3 direction, vec3 normal) {
	float diffuseFactor = dot(normal, -direction);
	vec4 diffuseColor = vec4(0, 0, 0, 0);
	if(diffuseFactor > 0) {
		diffuseColor = vec4(lightColor, 1.0) * sunIntensity * diffuseFactor;

		//vec3 directionToEye = normalize(eyePos - worldPos0);
		//vec3 reflectDirection = normalize(reflect(direction, normal));
		//float specularFactor = dot(directionToEye, reflectDirection);
		//specularFactor = pow(specularFactor, specularPower);
		//if(specularFactor > 0) {
		//	specularColor = vec4(base.color, 1) * specularIntensity * specularFactor * texture(specularMap, texCoord0.xy).r;
		//}
	}

	return diffuseColor;
}

// Main method.
void main() {
    vec3 ambient = color * 0.1;
    vec3 diffuse = calcLight(sunColor, -sunDirection, normal).xyz * color;
    color0 = ambient + diffuse;

    vec4 worldPosition = model * vec4(position, 1);
    vec4 positionRelativeToCamera = view * worldPosition;
	gl_Position =  projection * positionRelativeToCamera;

	float distance = length(positionRelativeToCamera.xyz);
	visibility0 = exp(-pow((distance*fogDensity), fogGradient));
	visibility0 = clamp(visibility0, 0.0, 1.0);
}