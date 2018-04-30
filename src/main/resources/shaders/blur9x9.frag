#version 330
#pragma optionNV (unroll all)

in vec2 texCoord0;
uniform float bloomDivider = 1;

uniform vec2 blurScale;
uniform sampler2D filterTexture;

const float weight11[11] = float[](
0.000003,	0.000229,	0.005977,	0.060598,	0.24173,	0.382925,	0.24173,	0.060598,	0.005977,	0.000229,	0.000003
);
const float weight9[9] = float[](
    0.000229,	0.005977,	0.060598,	0.241732,	0.382928,	0.241732,	0.060598,	0.005977,	0.000229
//  0           1           2           3           4           5           6           7           8
);
const float weight7[7] = float[](
0.00598,	0.060626,	0.241843,	0.383103,	0.241843,	0.060626,	0.00598
);



// -4 -3 -2 -1 0 1 2 3 4
//  0  1  2  3 4 5 6 7 8

void main() {
    vec4 color = vec4(0.0);
    vec2 tc = texCoord0 * bloomDivider;

    const int size = 4;

    // unroll please
    for (int i = -size; i <= size; i++) {
        color += texture(filterTexture, tc + (vec2(i, i) * blurScale.xy)) * (weight9[i+size]);
    }

    //color += texture(filterTexture, texCoord0 + (vec2(-3.0) * blurScale.xy)) * 0.015625;
    //color += texture(filterTexture, texCoord0 + (vec2(-2.0) * blurScale.xy)) * 0.09375;
    //color += texture(filterTexture, texCoord0 + (vec2(-1.0) * blurScale.xy)) * 0.234375;
    //color += texture(filterTexture, texCoord0 + (vec2(0.0)  * blurScale.xy)) * 0.3125;
    //color += texture(filterTexture, texCoord0 + (vec2(1.0)  * blurScale.xy)) * 0.234375;
    //color += texture(filterTexture, texCoord0 + (vec2(2.0)  * blurScale.xy)) * 0.09375;
    //color += texture(filterTexture, texCoord0 + (vec2(3.0)  * blurScale.xy)) * 0.015625;

    gl_FragColor = color;
}
