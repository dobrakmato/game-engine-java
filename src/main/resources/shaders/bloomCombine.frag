#version 330

const int BLOOM_TEXTURES = 6;

uniform sampler2D textures[BLOOM_TEXTURES];
uniform float weights[BLOOM_TEXTURES];
uniform float offsetsX[BLOOM_TEXTURES];
uniform float offsetsY[BLOOM_TEXTURES];

in vec2 texCoord0;

void main() {
    vec3 color = vec3(0);

    // unroll please
    for (int i = 0; i < BLOOM_TEXTURES; i++) {
        color += texture(textures[i], texCoord0 + vec2(offsetsX[i], offsetsY[i])).rgb * weights[i];
    }

    gl_FragColor = vec4(color * 0.33, 1);
}
