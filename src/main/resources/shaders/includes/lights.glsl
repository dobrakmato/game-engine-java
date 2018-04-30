#ifndef INCLUDE_LIGHTS
#define INCLUDE_LIGHTS

/* ======= LIGHT STRUCTS ======= */
struct Attenuation {
	float constant;
	float linear;
	float quadratic;
};
struct DirectionalLight {
	vec3 color;
	vec3 direction;
    float intensity;
};
struct PointLight {
	Attenuation atten;
	vec3 color;
	vec3 position;
    float intensity;
	float range;
};
struct SpotLight {
    Attenuation atten;
	vec3 color;
	vec3 position;
	vec3 direction;
    float intensity;
	float range;
	float cutoff;
};

#endif