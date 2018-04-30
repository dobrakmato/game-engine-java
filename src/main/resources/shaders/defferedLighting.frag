#version 330

#include "includes/shadows.glsl"
#include "includes/lights.glsl"

#define PI 3.1415926f
#define EPSILON 10e-5f

/*
	                R	        G	        B	        A
D	                Depth			                    Stencil
C1 (GL_RGBA)	    Albedo.R	Albedo.G	Albedo.B	Occlusion
C2 (GL_RGB10_A2)    Normal.X	Normal.Y	Normal.Z
C3 (GL_RGBA)	    Emissive	Roughness	Metalic
C4 (GL_RGBA)	    Position.X	Position.Y	Position.Z
*/

uniform sampler2D c0;      // color_attachment_0 texture 0
uniform sampler2D c1;      // color_attachment_1 texture 1
uniform sampler2D c2;      // color_attachment_2 texture 2
uniform sampler2D c3;      // color_attachment_3 texture 3

uniform bool castingShadows;

uniform sampler2DShadow shadowMap; // texture 8

uniform mat4 model;      // identity
uniform mat4 view;       // identity
uniform mat4 projection; // identity

uniform mat4 lightSpaceMatrix;

uniform DirectionalLight light;
uniform vec3 cameraPos;
uniform float specularMix;

uniform int gScreenWidth;
uniform int gScreenHeight;

vec2 CalcTexCoord() {
   return gl_FragCoord.xy / vec2(gScreenWidth, gScreenHeight);
}

float fresnel_unreal(float VdotH, float F0) {
    return F0 + (1 - F0) * exp2( (-5.55473 * VdotH - 6.98316) * VdotH );
}

float Distribution_GGX(float hdotN, float alpha) { // same as in marmoset toolbag ggx
	// alpha is assumed to be roughness^2
	float a2 = alpha * alpha;
	float tmp = ( hdotN * hdotN ) * ( a2 - 1.0 ) + 1.0;
	return ( a2 / ( PI * tmp * tmp ) );
}

float g1(float NdotV, float k) {
    return NdotV / (NdotV * (1 - k) + k );
}

float visibility(float roughness, float NdotV, float NdotL) {
    float k = ((roughness + 1) * (roughness + 1))/8;
    return g1(NdotV, k) * g1(NdotL, k);
}

float ggx (vec3 N, vec3 V, vec3 L, float roughness, float F0) {
  float alpha = roughness*roughness;
  vec3 H = normalize(L - V);
  float dotLH = max(0.0, dot(L,H));
  float dotNH = max(0.0, dot(N,H));
  float dotNL = max(0.0, dot(N,L));
  float alphaSqr = alpha * alpha;
  float denom = dotNH * dotNH * (alphaSqr - 1.0) + 1.0;
  float D = alphaSqr / (3.141592653589793 * denom * denom);
  float F = F0 + (1.0 - F0) * pow(1.0 - dotLH, 5.0);
  float k = 0.5 * alpha;
  float k2 = k * k;
  return dotNL * D * F / (dotLH*dotLH*(1.0-k2)+k2);
}

void main() {
    // Calculate texcoord to sample gbuffer.
    vec2 texCoord = CalcTexCoord();

    // Sample gbuffer.
    vec4 v0 = texture(c0, texCoord);
    vec4 v1 = texture(c1, texCoord);
    vec4 v2 = texture(c2, texCoord);
    vec4 v3 = texture(c3, texCoord);

    // Resolve shader params.
    vec3 albedo = v0.rgb;
    vec3 normal = normalize((v1.xyz * 2) - vec3(1, 1, 1));
    vec3 worldPos = v3.xyz;

    float occlusion = clamp(v0.a, 0.0, 1.0);
    float emissive = clamp(v2.r, 0.0, 1.0);
    float roughness = clamp(v2.g, 0.0, 1.0);
    float metallic = clamp(v2.b, 0.0, 1.0);

    vec3 viewDir = normalize(cameraPos - worldPos);
    vec3 halfwayDir = normalize(light.direction + viewDir);

    float NdotV = dot(normal, viewDir);
    float NdotL = clamp(dot(normal, light.direction), 0.0, 1.0);
    float NdotH = clamp(dot(normal, halfwayDir), 0.0, 1.0);
    float LdotH = clamp(dot(light.direction, halfwayDir), 0.0, 1.0);
    float HdotN = clamp(dot(halfwayDir, normal), 0.0, 1.0);
    float VdotN = clamp(dot(viewDir, normal), 0.0, 1.0);
    float VdotH = clamp(dot(viewDir, halfwayDir), 0.0, 1.0);

    float lambert = clamp(dot(normal, light.direction), 0.0, 1.0);

    float shadow = 1.0;
    if (castingShadows) {
        vec4 shadowCoord0 = (lightSpaceMatrix * vec4(worldPos, 1));
        shadow = min(1.0, shadow_directional(shadowMap, shadowCoord0) + 0.33);
    }


    float alpha = max(roughness * roughness, 0.002);

    float D = Distribution_GGX( HdotN, alpha );
    float G = visibility(roughness, VdotN, NdotL);
    float F = fresnel_unreal(NdotV, 0.04);

	float horizon = 1.0 - NdotL;
	horizon *= horizon;
	horizon *= horizon;
	vec3 specLightColor = light.color.rgb - light.color.rgb * horizon;

    //float G1 = NdotL + sqrt( 1.0 + pow(alpha,2) * (( 1.0 - pow(NdotL,2) ) / ( pow(NdotL,2) )) );
    //float G2 = NdotV + sqrt( 1.0 + pow(alpha,2) * (( 1.0 - pow(NdotV,2) ) / ( pow(NdotV,2) )) );

	//vec3 specularColor = clamp( D * G * ( F * ( specLightColor.rgb * lambert ) ) , 0.0, 1.0);
	float specularFactor = (D * G * F ) / 3.14159; // (PI * VdotN * NdotL);


	//specularFactor = D * F / (G1 * G2); // Full BDRF should be for Smith GGX - http://gamedev.stackexchange.com/a/128124/24749


	vec3 specularColor = specularFactor * light.color * light.intensity;
	//specularColor = (specularColor) * specularFactor;
    specularColor = mix(specularColor, specularColor * albedo, metallic);

    // Visibility_Schlick(float vdotN, float ldotN, float alpha)

	//lambert /= PI;

	vec3 diffuseColor = albedo * light.color * light.intensity * lambert;


    diffuseColor.rgb *= ( 1.0 - metallic );

    specularColor = mix(vec3(0), specularColor, specularMix);

    gl_FragColor.rgb = (diffuseColor + specularColor) * shadow;


    //float s = ggx(normal, viewDir, vec3(-light.direction.x, light.direction.y, -light.direction.z), roughness, 0.04);
    //float d = lambert;
    //vec3 sc = light.color * s * light.intensity;
    //vec3 scm = light.color * s * light.intensity * albedo;
    //sc = mix(sc, scm, metallic);
    //vec3 dc = albedo * light.color * light.intensity * lambert;
    //dc.rgb *= ( 1.0 - metallic );
    //sc = mix(vec3(0), sc, specularMix);
//
    //gl_FragColor.rgb = (dc + sc) * shadow;


    gl_FragColor.a = 1.0;
}