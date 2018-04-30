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
import eu.matejkormuth.math.vectors.Vector3f;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PointLight extends Light {

    @Getter
    private Vector3f position = new Vector3f(0, 0, 0);
    @Getter
    private Attenuation attenuation = new Attenuation(0, 0, 1f); // new Attenuation(1.0f, 0.014f, 0.0007f);

    @Getter
    private float range;

    private Matrix4f transformMatrix;
    private boolean transformMatrixDirty = true;

    private double i = Math.random();
    private double j = Math.random();
    private double k = Math.random();

    private double p = Math.random();
    private double o = Math.random();
    private double q = Math.random();

    public boolean animate = true;

    public void setPosition(Vector3f position) {
        this.position = position;
        transformMatrixDirty = true;
    }

    @Override
    protected void recalculateLightSpaceMatrix() {
        // makes no sense
    }

    public void setAttenuation(Attenuation attenuation) {
        this.attenuation = attenuation;
        this.recalculateRange();
    }

    private void recalculateRange() {
        float a = attenuation.getQuadratic();
        float b = attenuation.getLinear();
        float c = attenuation.getConstant() - 256 * getIntensity() * Math.max(color.getX(), Math.max(color.getY(), color.getZ()));

        this.range = (float) ((-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a)) * 1.2f;
        log.info("New range for {} is {}.", this, this.range);
    }

    @Override
    public void setUniforms(Program program) {
        program//.setUniform("lightSpaceMatrix", this.lightSpaceMatrix)
                .setUniform("light.color", this.color)
                .setUniform("light.intensity", this.intensity)
                .setUniform("light.position", this.position)
                .setUniform("light.atten.quadratic", this.attenuation.getQuadratic())
                .setUniform("light.atten.linear", this.attenuation.getLinear())
                .setUniform("light.atten.constant", this.attenuation.getConstant());
        //.setUniform("castingShadows", this.castingShadows);
    }

    public Matrix4f getTransformMatrix() {
        if (transformMatrixDirty) {
            recalculateTransformMatrix();
        }
        return transformMatrix;
    }

    private void recalculateTransformMatrix() {
        Matrix4f identity = new Matrix4f().initIdentity();
        Matrix4f translationMatrix = new Matrix4f().initTranslation(position.getX(), position.getY(), position.getZ());
        Matrix4f scaleMatrix = new Matrix4f().initScale(range, range, range);

        transformMatrix = translationMatrix.multiply(scaleMatrix.multiply(identity));
        //transformMatrixDirty =false;
    }

    private double time = 0;

    public void update(float delta) {
        time += delta;
        animate = false;
        if(animate) {
            this.position = new Vector3f(
                    (float) (Math.sin(time * (p + .5f) * 0.0004f + p * Math.PI) * i * 800),
                    (float) (Math.cos(time * (q + .5f) * 0.0004f + q * Math.PI) * j * 10 + 10f),
                    (float) (Math.sin(time * (o + .5f) * 0.0004f + o * Math.PI) * k * 800)
            );
        }
    }
}
