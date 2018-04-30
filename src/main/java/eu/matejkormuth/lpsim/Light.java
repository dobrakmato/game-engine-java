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
import lombok.Setter;

public abstract class Light {

    enum LightMobility {
        STATIC,
        DYNAMIC
    }

    @Getter
    @Setter
    protected float intensity = 1.0f;
    @Getter
    @Setter
    protected Vector3f color = new Vector3f(1, 1, 0.9f);
    @Getter
    @Setter
    protected LightMobility mobility = LightMobility.DYNAMIC;
    @Getter
    protected boolean castingShadows = false;

    //@Getter
    //protected ShadowInfo shadowInfo;

    @Deprecated
    @Getter
    protected ShadowMap shadowMap;

    @Getter
    protected Matrix4f lightProjection = new Matrix4f();
    @Getter
    protected Matrix4f lightView = new Matrix4f();

    protected Matrix4f lightSpaceMatrix;

    protected boolean lightSpaceMatrixDirty = true;

    public Matrix4f getLightSpaceMatrix() {
        if (lightSpaceMatrixDirty) {
            recalculateLightSpaceMatrix();
        }
        return lightSpaceMatrix;
    }

    protected abstract void recalculateLightSpaceMatrix();

    public void setCastingShadows(boolean castingShadows) {
        this.castingShadows = castingShadows;
        if (castingShadows) {
            if(this instanceof DirectionalLight) {
                this.shadowMap = new ShadowMap(4096);
            } else if(this instanceof SpotLight) {
                this.shadowMap = new ShadowMap(4096);
            } else {
                this.shadowMap = new ShadowMap(2048);
            }
        } else {
            this.shadowMap = null;
        }
    }

    public abstract void setUniforms(Program program);

    // directional light
    // point light
    // spot light

    // area light
    // sky(box/dome) light
}
