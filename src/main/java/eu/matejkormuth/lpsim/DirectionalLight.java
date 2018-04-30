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

public class DirectionalLight extends Light {

    @Getter
    protected Vector3f direction;

    @Getter
    private VSM vsm;

    public DirectionalLight() {
        this.lightProjection = new Matrix4f().initOrtho(-1024, 1024, -1024, 1024, -768, 768);
    }

    public void setDirection(Vector3f direction) {
        this.direction = direction;
        this.lightSpaceMatrixDirty = true;
    }

    @Override
    public void setCastingShadows(boolean castingShadows) {
        if (castingShadows) {
            vsm = new VSM(4096);
            this.castingShadows = true;
        }
    }

    private Matrix4f translationMatrix = new Matrix4f();

    private int roundToNearest(int n, int m) {
        int r = n % m;
        if (r == 0) {
            return n;
        }
        if (n < 0) {
            return -(Math.abs(n) - r);
        } else {
            return n + m - r;
        }
    }

    @Override
    protected void recalculateLightSpaceMatrix() {
        Matrix4f.initOrtho(this.lightProjection, -1024, 1024, -1024, 1024, -768, 768);
        Matrix4f.initCamera(this.lightView, direction.negate(), Vector3f.UNIT_Y);
        Vector3f translation = Application.get().getWorld().getCamera().getPosition().negate();
        Matrix4f.initTranslation(translationMatrix, roundToNearest((int) translation.getX(), 1), 0, roundToNearest((int) translation.getZ(), 1));

        this.lightSpaceMatrix = this.lightProjection.multiply(lightView.multiply(translationMatrix));
        this.lightSpaceMatrixDirty = false;
    }

    protected void recalculateLightSpaceMatrix2() {
        double shadowMapSize = 4096;
        double shadowMapArea = 1024;

        double quantizationStep = 1.0 / shadowMapSize;

        Vector3f camera = Application.get().getWorld().getCamera().getPosition().negate();

        double x = camera.getX();
        double y = camera.getZ();

        double minX = x - shadowMapArea;
        double minY = y - shadowMapArea;

        double maxX = x + shadowMapArea;
        double maxY = y + shadowMapArea;

        double qx = Math.IEEEremainder(minX, quantizationStep);
        double qy = Math.IEEEremainder(minY, quantizationStep);

        minX -= qx;
        minY -= qy;

        maxX += shadowMapSize;
        maxY += shadowMapSize;

        Matrix4f.initOrtho(this.lightProjection, (float) minX, (float) maxX, (float) minY, (float) maxY, -768, 768);
        Matrix4f.initCamera(this.lightView, direction.negate(), Vector3f.UNIT_Y);
        //Matrix4f.initTranslation(translationMatrix, translation.getX(), 0, translation.getZ());

        this.lightSpaceMatrix = this.lightProjection.multiply(lightView);
        this.lightSpaceMatrixDirty = false;
    }

    @Override
    public void setUniforms(Program program) {
        program.setUniform("light.color", this.color)
                .setUniform("light.intensity", this.intensity)
                .setUniform("light.direction", this.direction)
                .setUniform("castingShadows", this.castingShadows);

        if (castingShadows) {
            program.setUniform("lightSpaceMatrix", this.lightSpaceMatrix);
        }
    }
}
