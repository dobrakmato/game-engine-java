#ifndef INCLUDE_SHADOWS
#define INCLUDE_SHADOWS

const int SHADOW_SAMPLES = 12;

const vec3 poissonDisk[16] = vec3[](
   vec3(0.94558609, -0.76890725, 0),
   vec3(0.97484398, 0.75648379, 0),
   vec3(-0.94201624, -0.39906216, 0),
   vec3(-0.094184101, -0.92938870, 0),

   vec3(0.34495938, 0.29387760, 0),
   vec3(-0.81544232, -0.87912464, 0),
   vec3(-0.38277543, 0.27676845, 0),
   vec3(0.44323325, -0.97511554, 0),
   vec3(0.53742981, -0.47373420, 0),
   vec3(-0.26496911, -0.41893023, 0),
   vec3(0.79197514, 0.19090188, 0),
   vec3(-0.24188840, 0.99706507, 0),
   vec3(-0.81409955, 0.91437590, 0),
   vec3(0.19984126, 0.78641367, 0),
   vec3(-0.91588581, 0.45771432, 0),
   vec3(0.14383161, -0.14100790, 0)
);

const float DISTANT_SAMPLE = 1.0 / 600.0;

const vec3 DISTANT_SAMPLES[4] = vec3[] (
    vec3(DISTANT_SAMPLE, DISTANT_SAMPLE, 0),
    vec3(DISTANT_SAMPLE, -DISTANT_SAMPLE, 0),
    vec3(-DISTANT_SAMPLE, -DISTANT_SAMPLE, 0),
    vec3(-DISTANT_SAMPLE, DISTANT_SAMPLE, 0)
);

float shadow_smooth(sampler2DShadow depthMap, vec3 depthMapCoords) {
    float sample0 = texture(depthMap, depthMapCoords.xyz);
    float sample1 = texture(depthMap, depthMapCoords.xyz + DISTANT_SAMPLES[0]);
    float sample2 = texture(depthMap, depthMapCoords.xyz + DISTANT_SAMPLES[1]);
    float sample3 = texture(depthMap, depthMapCoords.xyz + DISTANT_SAMPLES[2]);
    float sample4 = texture(depthMap, depthMapCoords.xyz + DISTANT_SAMPLES[3]);

    float samples = sample0 + sample1 + sample2 + sample3 + sample4;

    // If distant samples are equally shadowed, we don't do soft shadows.
    if(abs(sample1 * 5 - samples) < 0.001) {
        return sample1;
    }

    float shadow = samples;
    for (int i = 3; i < SHADOW_SAMPLES - 5; i++) {
        shadow += texture(depthMap, depthMapCoords.xyz + poissonDisk[i] / 600.0); // divider should be shadow map resolution dependant
    }
    return shadow / SHADOW_SAMPLES;
}

float shadow_smooth2(sampler2DShadow depthMap, vec3 depthMapCoords) {
    float samples = 0;
    for(int i = 0; i < 16; i++) {
        samples += texture(depthMap, depthMapCoords.xyz + poissonDisk[i] / 1024.0);
    }
    return samples / 16;
}

float shadow_hard(sampler2DShadow depthMap, vec3 depthMapCoords) {
    return texture(depthMap, depthMapCoords.xyz);
}

float shadow_directional(sampler2DShadow shadowMap, vec4 shadowCoord) {
    vec3 projCoords = shadowCoord.xyz / shadowCoord.w;
    projCoords = projCoords * 0.5 + 0.5;

    if (projCoords.z > 1.0) {
        return 0.0;
    }

    return shadow_smooth2(shadowMap, projCoords.xyz);
}

float shadow_spot(sampler2DShadow shadowMap, vec4 shadowCoord, float cosTheta) {
    //float bias = 0.005*tan(acos(cosTheta)); // cosTheta is dot( n,l ), clamped between 0 and 1
    //bias = clamp(bias, 0,0.01);

    //vec3 projCoords = shadowCoord.xyz / shadowCoord.w;
    //projCoords = projCoords * 0.5 + 0.5;
//
    //if (projCoords.z > 1.0) {
    //    return 0.0;
    //}

    //if(texture( shadowMap, shadowCoord.xy).r < shadowCoord.z) {
    //    return 0.0;
    //}
    return 1.0;
}

#endif