#ifndef INCLUDE_BUMP_MAPPING
#define INCLUDE_BUMP_MAPPING

vec3 normalmap_normal(mat3 tbn, sampler2D normalMap, vec2 texCoord) {
    vec3 bumpMapNormal = (texture(normalMap, texCoord).xyz * 2) - vec3(1, 1, 1);
    return normalize(tbn * bumpMapNormal);
}

#endif