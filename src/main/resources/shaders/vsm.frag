#version 330

void main() {
    //float depth = v_position.z / v_position.w;
    //depth = depth * 0.5 + 0.5;
    float depth = gl_FragCoord.z;

	float moment1 = depth;
	float moment2 = depth * depth;

	float dx = dFdx(depth);
	float dy = dFdy(depth);
	moment2 += 0.25 * (dx * dx + dy * dy);

	gl_FragColor = vec4(moment1, moment2, 0.0, 0.0);
}
