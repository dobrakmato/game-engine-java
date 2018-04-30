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

/*
	                R	        G	        B	        A
D	                Depth			                    Stencil
C1 (GL_RGBA)	    Albedo.R	Albedo.G	Albedo.B	Occlusion
C2 (GL_RGB10_A2)    Normal.X	Normal.Y	Normal.Z    Metalic
C3 (GL_RGBA)	    Emissive	Roughness
*/


uniform sampler2D c0;      // color_attachment_0 texture 0
uniform sampler2D c1;      // color_attachment_1 texture 1
uniform sampler2D c2;      // color_attachment_2 texture 2
//uniform sampler2D c3;      // color_attachment_3 texture 3

uniform sampler2D depth;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

in mat4 invView;
in mat4 invProj;

uniform PointLight light;
uniform vec3 cameraPos;
uniform float specularMix;

uniform int gScreenWidth;
uniform int gScreenHeight;

vec2 CalcTexCoord() {
   return gl_FragCoord.xy / vec2(gScreenWidth, gScreenHeight);
}

vec3 reconstruct_position(float z, vec2 texCoord) {
    z = z * 2.0 - 1.0;
    //mat4 invProj = inverse(projection);
    //mat4 invView = inverse(view);

    vec4 clipPos = vec4(texCoord * 2.0 - 1.0, z, 1.0);
    vec4 viewPos = invProj * clipPos;
    viewPos  /= viewPos.w;
    vec4 worldPos = invView * viewPos;

    return (worldPos.xyz);
}

vec3 shadingSpecularGGX(vec3 N, vec3 V, vec3 L, float roughness, vec3 F0)
{
    // see http://www.filmicworlds.com/2014/04/21/optimizing-ggx-shaders-with-dotlh/
    vec3 H = normalize(V + L);
    float dotLH = min(1.0, max(dot(L, H), 0.0));
    float dotNH = min(1.0, max(dot(N, H), 0.0));
    float dotNL = min(1.0, max(dot(N, L), 0.0));
    float dotNV = min(1.0, max(dot(N, V), 0.0));
    float alpha = roughness * roughness;
    // D (GGX normal distribution)
    float alphaSqr = alpha * alpha;
    float denom = dotNH * dotNH * (alphaSqr - 1.0) + 1.0;
    float D = alphaSqr / (denom * denom);
    // no pi because BRDF -> lighting
    // F (Fresnel term)
    float F_a = 1.0;
    float Fbt = 1.0 - dotLH;
    float F_b = Fbt*Fbt*Fbt*Fbt*Fbt*Fbt; // pow(1.0 - dotLH, 6); <- This produces NaN in some cases which causes artefacts.
    vec3 F = mix(vec3(F_b), vec3(F_a), F0);
    // G (remapped hotness, see Unreal Shading)
    float k = (alpha + 2 * roughness + 1) / 8.0;
    float G = dotNL / (mix(dotNL, 1, k) * mix(dotNV, 1, k));
    // '* dotNV' - canceled by normalization
    // orginal G:
    /*
    {
        float k = alpha / 2.0;
        float k2 = k * k;
        float invK2 = 1.0 - k2;
        float vis = 1 / (dotLH * dotLH * invK2 + k2);
        vec3 FV = mix(vec3(F_b), vec3(F_a), F0) * vis;
        vec3 specular = D * FV / 4.0f;
        return specular * dotNL;
    }
    */
    // '/ dotLN' - canceled by lambert
    // '/ dotNV' - canceled by G
    return D * F * G / 4.0;
}

void main() {
    // Calculate texcoord to sample gbuffer.
    vec2 texCoord = CalcTexCoord();

    float z = texture(depth, texCoord).x;
    vec3 worldPos = reconstruct_position(z, texCoord);

    // Sample gbuffer.
    vec4 v0 = texture(c0, texCoord).rgba;
    vec3 v1 = texture(c1, texCoord).rgb;
    vec2 v2 = texture(c2, texCoord).gb;

    // Resolve shader params.
    vec3 albedo = v0.rgb;
    vec3 normal = normalize((v1.xyz * 2) - vec3(1, 1, 1));

    vec3 viewDir = normalize(cameraPos - worldPos);
    vec3 lightDir = normalize(light.position - worldPos);

    float occlusion = clamp(v0.a, 0.0, 1.0);
    //float emissive = clamp(v2.r, 0.0, 1.0);
    float roughness = clamp(v2.r, 0.0, 1.0);
    float metallic = clamp(v2.g, 0.0, 1.0);

    float lambert = max(0.0, dot(lightDir, normal));
    vec3 specular = mix(vec3(0.04), albedo, metallic);
    vec3 diffuse = albedo * (1 - specular);

    float distance = length(light.position - worldPos);
    float attenuation = clamp(1.0 / (distance * distance * light.atten.quadratic +
                               distance * light.atten.linear +
                               light.atten.constant), 0.0, 1.0);

    gl_FragColor.rgb = (diffuse * lambert * light.color * light.intensity) + shadingSpecularGGX(normal, viewDir, lightDir, roughness, specular) * light.color;

    gl_FragColor.rgb *= attenuation;
    gl_FragColor.a = 1.0;
}