#version 330

#include "includes/bump_mapping.glsl"

#define UseToksvig_

// Input
in vec2 texCoord0;
//in vec3 normal0;
in vec3 tangent0;
in vec4 worldPos0;

in vec3 N;
in vec3 E;

in mat3 TBN0;

uniform sampler2D map_albedo;           // texture 0
uniform sampler2D map_roughness;        // texture 1
uniform sampler2D map_metalic;          // texture 2
uniform sampler2D map_normal;           // texture 3
uniform sampler2D map_ambientOcclusion; // texture 4

uniform sampler2D map_height; // texture 5
uniform sampler2D map_emissive; // texture 6

uniform vec3 albedoColor = vec3(1, 1, 1);
uniform bool invertRoughness = false;

uniform bool G_POM = false;
uniform bool G_EMISSIVE = false;
uniform bool G_OCCLUSION = false;
uniform bool G_ANISOTROPIC = false;

uniform vec3 cameraPos;
uniform float height_scale = 0.04;

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
//layout (location = 3) out vec4 c3; // color_attachment_4

vec2 ParallaxMapping(vec2 texCoords, vec3 viewDir) {
        // number of depth layers
        const float minLayers = 8; //8
        const float maxLayers = 32;  //32
        float numLayers = mix(maxLayers, minLayers, abs(dot(vec3(0.0, 0.0, 1.0), viewDir)));


        // calculate the size of each layer
        float layerDepth = 1.0 / numLayers;
        // depth of current layer
        float currentLayerDepth = 0.0;
        // the amount to shift the texture coordinates per layer (from vector P)
        vec2 P = viewDir.xy / viewDir.z * height_scale;
        vec2 deltaTexCoords = P / numLayers;

        // get initial values
        vec2  currentTexCoords     = texCoords;
        float currentDepthMapValue = 1 - texture(map_height, currentTexCoords).r;

        while(currentLayerDepth < currentDepthMapValue)
        {
            // shift texture coordinates along direction of P
            currentTexCoords -= deltaTexCoords;
            // get depthmap value at current texture coordinates
            currentDepthMapValue = 1 - texture(map_height, currentTexCoords).r;
            // get depth of next layer
            currentLayerDepth += layerDepth;
        }

        // -- parallax occlusion mapping interpolation from here on
        // get texture coordinates before collision (reverse operations)
        vec2 prevTexCoords = currentTexCoords + deltaTexCoords;

        // get depth after and before collision for linear interpolation
        float afterDepth  = currentDepthMapValue - currentLayerDepth;
        float beforeDepth = (1 - texture(map_height, prevTexCoords).r) - currentLayerDepth + layerDepth;

        // interpolation of texture coordinates
        float weight = afterDepth / (afterDepth - beforeDepth);
        vec2 finalTexCoords = prevTexCoords * weight + currentTexCoords * (1.0 - weight);

        return finalTexCoords;
}


uniform float pomHeightScale = 0.04; // between 0.01 - 0.1
uniform float minSamples = 8;
uniform float maxSamples = 32;

// https://www.gamedev.net/resources/_/technical/graphics-programming-and-theory/a-closer-look-at-parallax-occlusion-mapping-r3262
// http://sunandblackcat.com/tipFullView.php?topicid=28
vec2 pom(vec2 texCoords, vec3 e, vec3 n) {
    float steps = mix(maxSamples, minSamples, abs(dot(n, e)));

    float stepHeight = 1.0 / steps;

    float currentStepHeight = 0.0;
    vec2 dTex = pomHeightScale * ((e.xy / e.z) / steps);

    vec2 currentTexCoords = texCoords;
    float heightFromTexture = 1 -texture(map_height, currentTexCoords).r;

    while (heightFromTexture > currentStepHeight) {
        currentStepHeight += stepHeight;
        currentTexCoords -= dTex;
        heightFromTexture = 1 - texture(map_height, currentTexCoords).r;
    }

    vec2 prevTexCoords = currentTexCoords + dTex;

    // lerp
    float nextH = heightFromTexture - currentStepHeight;
    float prevH = (1 - texture(map_height, prevTexCoords).r)  - currentStepHeight + stepHeight;

    float weight = nextH / (nextH + prevH);
    vec2 finalTexCoords = prevTexCoords * weight + currentTexCoords * (1.0 - weight);

    return finalTexCoords;
}

float RoughnessToSpecPower(in float m) {
    return 2.0 / (m * m) - 2.0;
}

float SpecPowerToRoughness(in float s) {
    return sqrt(2.0 / (s + 2.0));
}

void main() {
    vec2 texCoord = texCoord0;
    if (G_POM) { // Parralax occlusion mapping
        //vec3 viewDir = normalize(tangentViewPos - tangentFragPos);
        //vec3 viewDir = normalize(tangentViewPos);
        vec2 t = texCoord;
        texCoord = ParallaxMapping(texCoord0, normalize(E)); //pom(t, normalize(E), normalize(N));
    }
    vec3 albedo = texture(map_albedo, texCoord).rgb;
    float roughness = texture(map_roughness, texCoord).r;
    float metalic = texture(map_metalic, texCoord).r;

    float occlusion = 0.0;
    float emissive = 0;
    vec3 normal = vec3(0);

    if (G_OCCLUSION) {
        occlusion = texture(map_ambientOcclusion, texCoord).r;
    }

    if (G_EMISSIVE) {
       emissive = texture(map_emissive, texCoord).r;
    }

    if (G_ANISOTROPIC) {
        normal = tangent0;
    } else {
        normal = normalmap_normal(TBN0, map_normal, texCoord).xyz; // normal mapping
    }

    #ifdef UseToksvig_
        float s = RoughnessToSpecPower(roughness);
        float normalMapLen = length(normal);
        float ft = normalMapLen / mix(s, 1.0, normalMapLen);
        ft = max(ft, 0.01);
        roughness = SpecPowerToRoughness(ft * s);
    #endif

    c0 = vec4(albedo, occlusion);
    c1.xyz = (normal + vec3(1, 1, 1)) / 2;
    c2.rgb = vec3(emissive, roughness, metalic);
    //c3.rgb = worldPos0.rgb;
}