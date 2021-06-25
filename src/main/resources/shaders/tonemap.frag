#version 330

in vec2 texCoord0;

uniform float exposure;
uniform sampler2D tex;

vec3 tonemap_hejl(vec3 hdr, float whitePt) {
    vec4 vh = vec4(hdr, whitePt);
    vec4 va = (1.425 * vh) + 0.05f;
    vec4 vf = ((vh * va + 0.004f) / ((vh * (va + 0.55f) + 0.0491f))) - 0.0821f;
    return vf.rgb / vf.www;
}

vec3 ACESFilm( vec3 x )
{
    float a = 2.51f;
    float b = 0.03f;
    float c = 2.43f;
    float d = 0.59f;
    float e = 0.14f;
    return clamp((x*(a*x+b))/(x*(c*x+d)+e), vec3(0), vec3(1));
}

void main() {
    vec3 hdr = texture(tex, texCoord0).rgb;
    //if (hdr.r > 1 || hdr.g > 1 || hdr.b > 1) {
    //    hdr = vec3(1,0,0);
    //}

    vec3 color = tonemap_hejl(hdr * exposure, 50);
    //vec3 color = ACESFilm(hdr*0.6);
    gl_FragColor = vec4(color, 1);
}