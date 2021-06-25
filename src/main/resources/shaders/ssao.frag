#version 330

#define PI 3.1415926f
#define EPSILON 10e-5f

/*
	                R	        G	        B	        A
D	                Depth			                    Stencil      32 bits
C1 (GL_RGBA)	    Albedo.R	Albedo.G	Albedo.B	Occlusion    32 bits
C2 (GL_RGB10_A2)    Normal.X	Normal.Y	Normal.Z                 32 bits
C3 (GL_RGBA)	    Emissive	Roughness	Metalic                  32 bits
*/

uniform sampler2D c1;// color_attachment_1 texture 1
uniform sampler2D depth; // texture 9

uniform mat4 projection;
uniform mat4 view;

in mat4 invProj;
in mat4 invView;

// ssao variables
const int KERNEL_SAMPLES = 64;
const float RADIUS = 0.5f;
const float bias = 0.025f;

uniform vec2 noiseScale;
uniform sampler2D texNoise;// texture 11
uniform vec3 samples[KERNEL_SAMPLES];

in vec2 texCoord0;

vec3 reconstruct_view_position(float z, vec2 texCoord) {
    z = z * 2.0 - 1.0;

    vec4 clipPos = vec4(texCoord * 2.0 - 1.0, z, 1.0);
    vec4 viewPos = invProj * clipPos;
    viewPos  /= viewPos.w;
    //vec4 worldPos = invView * viewPos;

    return (viewPos.xyz);
}

void main() {
    float z = texture(depth, texCoord0).x;
    vec3 viewPos = reconstruct_view_position(z, texCoord0);
    //vec3 viewPos = (view * vec4(worldPos, 1)).xyz;

    vec3 v1 = texture(c1, texCoord0).xyz;
    vec3 worldNormal = normalize((v1.xyz * 2) - vec3(1, 1, 1));

    vec3 normal = normalize((view * vec4(worldNormal, 0)).xyz);
    vec3 randomVec = normalize(texture(texNoise, texCoord0 * noiseScale).xyz);

    vec3 tangent = normalize(randomVec - normal * dot(randomVec, normal));
    vec3 bitangent = cross(normal, tangent);
    mat3 TBN = mat3(tangent, bitangent, normal);

    // accumulate occlusion from multiple samples
    float occlusion = 0.0;
    for (int i = 0; i < KERNEL_SAMPLES; ++i)
    {
        // get sample position
        vec3 samplePos = TBN * samples[i];// from tangent to view-space
        samplePos = viewPos + samplePos * RADIUS;

        vec4 offset = vec4(samplePos, 1.0);
        offset      = projection * offset;    // from view to clip-space
        offset.xyz /= offset.w;               // perspective division
        offset.xyz  = offset.xyz * 0.5 + 0.5; // transform to range 0.0 - 1.0

        float sampleZ = texture(depth, offset.xy).x;

        vec4 blockerNDC = vec4(offset.xy * 2 - 1, sampleZ * 2 - 1, 1.0);
        vec4 blockerPos = invProj * blockerNDC;
        blockerPos  /= blockerPos.w;

         float rangeCheck = smoothstep(0.0, 1.0, RADIUS / abs(viewPos.z - blockerPos.z));
        occlusion += (blockerPos.z >= samplePos.z + bias ? 1.0 : 0.0) * rangeCheck;

    }

    occlusion = 1.0 - (occlusion / KERNEL_SAMPLES);
    gl_FragColor =  vec4(occlusion, occlusion, occlusion, 0); //vec4(occlusion, 1);
}