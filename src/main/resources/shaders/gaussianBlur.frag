#version 330

in vec2 texCoord0;

uniform vec2 blurScale;
uniform sampler2D filterTexture;

void main() {
    vec2 color = vec2(0.0);

    // Use only for blurring shadows!
    color += texture(filterTexture, texCoord0 + (vec2(-3.0) * blurScale.xy)).rg * 0.015625;
    color += texture(filterTexture, texCoord0 + (vec2(-2.0) * blurScale.xy)).rg * 0.09375;
    color += texture(filterTexture, texCoord0 + (vec2(-1.0) * blurScale.xy)).rg * 0.234375;
    color += texture(filterTexture, texCoord0 + (vec2(0.0)  * blurScale.xy)).rg * 0.3125;
    color += texture(filterTexture, texCoord0 + (vec2(1.0)  * blurScale.xy)).rg * 0.234375;
    color += texture(filterTexture, texCoord0 + (vec2(2.0)  * blurScale.xy)).rg * 0.09375;
    color += texture(filterTexture, texCoord0 + (vec2(3.0)  * blurScale.xy)).rg * 0.015625;

    gl_FragColor.rg = color;
}
