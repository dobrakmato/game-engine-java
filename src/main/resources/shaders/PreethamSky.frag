#version 330 core

#define PI 3.14159265359

in vec3 direction;

uniform vec3 A, B, C, D, E, Z;
uniform vec3 SunDirection;
uniform float turbidity;
uniform bool renderSun;

const vec3 UP = vec3(0, 1, 0);

vec3 YxyToXYZ(vec3 Yxy) {
	float Y = Yxy.r;
	float x = Yxy.g;
	float y = Yxy.b;

	float X = x * (Y / y);
	float Z = (1.0 - x - y) * (Y / y);

	return vec3(X, Y, Z);
}

vec3 XYZToRGB(vec3 XYZ) {
	// CIE/E
	mat3 M = mat3 (
		 2.3706743, -0.9000405, -0.4706338,
		-0.5138850,  1.4253036,  0.0885814,
 		 0.0052982, -0.0146949,  1.0093968
	);

	return XYZ * M;
}

vec3 YxyToRGB(vec3 Yxy) {
	vec3 XYZ = YxyToXYZ(Yxy);
	vec3 RGB = XYZToRGB(XYZ);
	return RGB;
}

float saturatedDot(vec3 a, vec3 b) {
	return max(dot(a, b), 0.0);
}

vec3 perez(float theta, float gamma, vec3 A, vec3 B, vec3 C, vec3 D, vec3 E) {
	return ( 1.0 + A * exp( B / cos( theta ) ) ) * ( 1.0 + C * exp( D * gamma ) + E * cos( gamma ) * cos( gamma ) );
}

vec3 calculateSkyLuminanceRGB(vec3 sunDir, vec3 viewDir) {
	float thetaS = acos(saturatedDot(sunDir, UP));
	float thetaE = acos(saturatedDot(viewDir, UP));
	float gammaE = acos(saturatedDot(sunDir, viewDir));

	vec3 fThetaGamma = perez(thetaE, gammaE, A, B, C, D, E);
	vec3 fZeroThetaS = perez(0.0,    thetaS, A, B, C, D, E);

	vec3 Yp = Z * (fThetaGamma / fZeroThetaS);

	return YxyToRGB(Yp);
}

vec3 calculateSunLuminance(vec3 sunDirection, vec3 sunColor, float turbidity, float ozone) {
    float cos_theta = saturatedDot(sunDirection, UP);

    float m = 1 / (cos_theta + 0.15 * pow(3.885 - cos_theta, -1.253));
    vec3 t_r = exp(-0.008735 * pow(sunColor, vec3(-4.08 * m)));

    const float alpha = 1.3;
    float beta = 0.04608 * turbidity - 0.04586;
    vec3 t_a = exp(-beta * pow(sunColor, vec3(-alpha * m)));

    //vec3 k_o = vec3(0.01127381, , );
    //vec3 t_o = exp(-k_o * 0.35 * m);

    //vec3 k_g = vec3(0);
    //vec3 t_g = exp(-1.41 * k_g * m / pow(1 + 118.93 * k_g * m, 0.45));

    //vec3 k_w = vec3(0);
    //vec3 t_w = exp(0.2385 * k_w * m / pow(1 + 20.07 * k_w * m, 0.45));

    vec3 color = sunColor * t_r * t_a; // * t_o * t_g * t_w;

    return XYZToRGB(color);
}

void main() {
    vec3 dir = normalize(direction);
    vec3 skyLuminance = calculateSkyLuminanceRGB(SunDirection, dir);

    if (renderSun) {
        vec3 sunLuminance = vec3(0,0,0);
        float howmuch = dot(SunDirection, dir);
        const float isEnough = 0.99965;
        if(howmuch > isEnough) {
            gl_FragColor = vec4(calculateSunLuminance(SunDirection, vec3(34, 31, 4), turbidity, 0.35), 1.0);
        } else {
            gl_FragColor = vec4(skyLuminance *0.3 , 1.0 );
        }
    } else {
        gl_FragColor = vec4(skyLuminance *0.3 , 1.0 );
    }


}
