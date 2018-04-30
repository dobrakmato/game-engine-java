#version 330

#include "includes/bump_mapping.glsl"

// Input
in vec2 texCoord0;
//in vec3 normal0;
in vec3 tangent0;
in vec4 worldPos0;

in mat3 TBN0;
in vec3 tintColor0;

uniform sampler2D map_albedo;           // texture 0
uniform sampler2D map_roughness;        // texture 1
uniform sampler2D map_metalic;          // texture 2
uniform sampler2D map_normal;           // texture 3
uniform sampler2D map_ambientOcclusion; // texture 4

uniform vec3 cameraPos;

/*
	                R	        G	        B	        A
D	                Depth			                    Stencil
C1 (GL_RGBA)	    Albedo.R	Albedo.G	Albedo.B	Occlusion
C2 (GL_RGB10_A2)    Normal.X	Normal.Y	Normal.Z
C3 (GL_RGBA)	    Emissive	Roughness	Metalic
*/

layout (location = 0) out vec4 c0; // color_attachment_1
layout (location = 1) out vec4 c1; // color_attachment_2
layout (location = 2) out vec4 c2; // color_attachment_3

uniform float roughnessColor;
uniform float alphaCutoff = 0.5;

void main() {
    vec2 texCoord = texCoord0;

    float alpha = texture(map_albedo, texCoord).a;
    if (alpha < alphaCutoff) {
        discard;
    }

    vec3 albedo = texture(map_albedo, texCoord).rgb * tintColor0;
    float roughness = texture(map_roughness, texCoord).r * roughnessColor;

    float occlusion = 0.0;
    float emissive = 0;

    vec3 normal = normalmap_normal(TBN0, map_normal, texCoord).xyz; // normal mapping

    c0 = vec4(albedo, occlusion);
    c1.xyz = (normal + vec3(1, 1, 1)) / 2;
    c1.a = 1;
    c2.rgb = vec3(emissive, roughness, 0.0);
}