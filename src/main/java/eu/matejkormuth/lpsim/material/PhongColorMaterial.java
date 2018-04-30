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
package eu.matejkormuth.lpsim.material;

import eu.matejkormuth.lpsim.*;
import eu.matejkormuth.lpsim.gl.Program;
import eu.matejkormuth.math.vectors.Vector3f;
import lombok.Getter;
import lombok.Setter;

public class PhongColorMaterial extends Material {

    @Getter
    @Setter
    private float specularPower = 32;
    @Getter
    @Setter
    private float specularIntensity = 0.6f;
    @Getter
    @Setter
    private Vector3f color = new Vector3f(1, 1, 1);

    public PhongColorMaterial(Syntax.Cfg cfg) {
        setProgram("phongColor");
        vertexLayout = InterleavedVertexLayout.builder()
                .attribute("position", InterleavedVertexLayout.AttributeType.VEC3)
                .attribute("normal", InterleavedVertexLayout.AttributeType.VEC3)
                .build();

        readConfig(cfg);
    }

    private void readConfig(Syntax.Cfg cfg) {
        specularIntensity = cfg.get("specularIntensity", 0.4f);
        specularPower = cfg.get("specularPower", 16f);
        color = cfg.get("color", new Vector3f(1, 1, 1));
    }

    @Override
    public void setUniforms(Program program) {
        program.setUniform("specularPower", specularPower)
                .setUniform("specularIntensity", specularIntensity)
                .setUniform("color", color);
    }

    @Override
    public void dispose() {
        throw new UnsupportedOperationException("not yet implemented!");
    }
}
