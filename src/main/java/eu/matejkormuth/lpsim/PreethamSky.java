/**
 * lpsim - 
 * Copyright (c) 2015, Matej Kormuth <http://www.github.com/dobrakmato>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package eu.matejkormuth.lpsim;

import eu.matejkormuth.lpsim.gl.Program;
import eu.matejkormuth.lpsim.math.Matrix4f;
import eu.matejkormuth.math.MathUtils;
import eu.matejkormuth.math.vectors.Vector3d;
import eu.matejkormuth.math.vectors.Vector3f;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import static eu.matejkormuth.lpsim.Syntax.vec3;
import static java.lang.Math.*;

@RequiredArgsConstructor
public class PreethamSky extends Sky {

    private final Mesh skydome;
    private final Camera camera;
    private final DirectionalLight sun;

    private Program skyProgram = ShaderCollection.provideProgram("PreethamSky");

    private Vector3f A;
    private Vector3f B;
    private Vector3f C;
    private Vector3f D;
    private Vector3f E;
    private Vector3f Z;

    @Getter
    @Setter
    private float turbidity = 2.0f;

    @Getter
    @Setter
    private float sunTheta = 45; // degrees (pitch)

    @Getter
    @Setter
    private float sunPhi = 200; // degrees (yaw)

    private void computeCoefficients() {
        A = vec3(0.1787f * turbidity - 1.4630f, -0.0193f * turbidity - 0.2592f, -0.0167f * turbidity - 0.2608f);
        B = vec3(-0.3554f * turbidity + 0.4275f, -0.0665f * turbidity + 0.0008f, -0.0950f * turbidity + 0.0092f);
        C = vec3(-0.0227f * turbidity + 5.3251f, -0.0004f * turbidity + 0.2125f, -0.0079f * turbidity + 0.2102f);
        D = vec3(0.1206f * turbidity - 2.5771f, -0.0641f * turbidity - 0.8989f, -0.0441f * turbidity - 1.6537f);
        E = vec3(-0.0670f * turbidity + 0.3703f, -0.0033f * turbidity + 0.0452f, -0.0109f * turbidity + 0.0529f);
    }

    private void computeZenith() {
        float thetaS = (float) toRadians(sunTheta);
        float chi = (float) ((4.0f / 9.0f - turbidity / 120.0f) * (PI - 2.0f * thetaS));
        float Yz = (float) ((4.0453f * turbidity - 4.9710f) * tan(chi) - 0.2155f * turbidity + 2.4192f);

        float theta2 = thetaS * thetaS;
        float theta3 = theta2 * thetaS;
        float T = turbidity;
        float T2 = turbidity * turbidity;

        float xz = (0.00165f * theta3 - 0.00375f * theta2 + 0.00209f * thetaS + 0.0f) * T2 +
                (-0.02903f * theta3 + 0.06377f * theta2 - 0.03202f * thetaS + 0.00394f) * T +
                (0.11693f * theta3 - 0.21196f * theta2 + 0.06052f * thetaS + 0.25886f);

        float yz = (0.00275f * theta3 - 0.00610f * theta2 + 0.00317f * thetaS + 0.0f) * T2 +
                (-0.04214f * theta3 + 0.08970f * theta2 - 0.04153f * thetaS + 0.00516f) * T +
                (0.15346f * theta3 - 0.26756f * theta2 + 0.06670f * thetaS + 0.26688f);

        Z = vec3(Yz, xz, yz);

    }

    private float perez(float theta, float gamma, float A, float B, float C, float D, float E) {
        double cos_theta = cos(theta);
        double cos_gamma = cos(gamma);
        return (float) ((1.0 + A * exp(B / cos_theta)) * (1.0 + C * exp(D * gamma) + E * cos_gamma * cos_gamma));
    }

    private Vector3f perez(float theta, float gamma, Vector3f A, Vector3f B, Vector3f C, Vector3f D, Vector3f E) {
        return new Vector3f(
                perez(theta, gamma, A.getX(), B.getX(), C.getX(), D.getX(), E.getX()),
                perez(theta, gamma, A.getY(), B.getY(), C.getY(), D.getY(), E.getY()),
                perez(theta, gamma, A.getZ(), B.getZ(), C.getZ(), D.getZ(), E.getZ())
        );
    }

    private Vector3f spherical(float theta, float phi) {
        return new Vector3d(cos(phi) * sin(theta), sin(phi) * sin(theta), cos(theta)).asVetor3f();
    }

    float time;

    public void render(Matrix4f wvp, boolean renderSun) {
        doRender(wvp, renderSun);
    }

    @Override
    public void render() {
        time += 0.0025f;
        sunTheta = (float) (45 + Math.sin(time) * 45);

        // Compute matrices.
        Matrix4f translation = new Matrix4f().initTranslation(camera.getPosition().getX(), camera.getPosition().getY(), camera.getPosition().getZ());
        Matrix4f scale = new Matrix4f().initScale(camera.getFar() * .99f, camera.getFar() * .99f, camera.getFar() * .99f);
        Matrix4f wvp = camera.getProjectionMatrix().multiply(camera.getViewMatrix().multiply(translation.multiply(scale)));

        doRender(wvp, true);
    }

    private void doRender(Matrix4f wvp, boolean renderSun) {
        computeCoefficients();
        computeZenith();

        // Compute sun direction.
        Vector3f direction = spherical((float) toRadians(sunTheta), (float) toRadians(sunPhi));
        direction = new Vector3f(direction.getX(), direction.getZ(), direction.getY());
        sun.setDirection(direction);

        // Compute sun intensity and color.
        computeSunColorAndIntensity();

        skyProgram.use()
                .setUniform("wvp", wvp)
                .setUniform("SunDirection", direction)
                .setUniform("renderSun", renderSun)
                .setUniform("A", A)
                .setUniform("B", B)
                .setUniform("C", C)
                .setUniform("D", D)
                .setUniform("E", E)
                .setUniform("Z", Z);

        skydome.drawElements();
    }

    private void computeSunColorAndIntensity() {
        final Vector3f down = new Vector3f(182 / 255f, 126 / 255f, 91 / 255f);
        final Vector3f up = new Vector3f(192 / 255f, 191 / 255f, 173 / 255f);

        float progress = sun.getDirection().dot(Vector3f.UNIT_Y);

        sun.setColor(Vector3f.lerp(down, up, progress));
        sun.setIntensity(MathUtils.lerp(0, 10, progress));
        //sun.setIntensity(0.0f);
    }


}
