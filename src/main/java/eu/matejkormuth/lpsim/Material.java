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
import lombok.Getter;

public abstract class Material extends ReferenceCounting {

    /**
     * Program (shader) this material uses.
     */
    @Getter
    private String program;

    // todo: wtf? this is related to forward shading...
    @Getter
    protected LightPrograms lightPrograms;

    /**
     * Vertex buffer layout as this material's program (shader) expects it.
     */
    @Getter
    protected InterleavedVertexLayout vertexLayout;

    protected void setProgram(String program) {
        this.program = program;
        this.lightPrograms = new LightPrograms(program);
    }

    /**
     * Sets required uniforms.
     */
    public abstract void setUniforms(Program program);

    /*
     * Dynamic Tessellation
PN Triangle mesh smoothing
Height & Vector Displacement modes
Detail Normal Maps
Parallax Occlusion Maps
"Metalness" Maps
GGX, Blinn-Phong, & Anisotropic Reflections
Secondary Reflections
Skin Diffusion
Microfiber ("Fuzz") Diffusion
Occlusion and Cavity Maps
Emissive Maps
Allegorithmic Substances
Dithered Supersampled Transparency
     */

    // blend mode (additive, etc...)

    // shading model: {lit - lights applied, unlit - only emmisive (+glow/bloom) used by lights (fire/bulb), subsurface?, clear coat?}

    // ambient color (vec3)
    // ambient map? (texture2d)

    // diffuse color (vec3)
    // diffuse map (texture2d)

    // emissive color (vec3) [https://docs.unrealengine.com/latest/INT/Engine/Rendering/Materials/HowTo/EmissiveGlow/]
    // emissive map (texture2d) ?

    // fresnel [https://docs.unrealengine.com/latest/INT/Engine/Rendering/Materials/HowTo/Fresnel/index.html]

    // occlusion map (self)

    // normal map (texture2d)
    // bump map
    // displacement map

    // reflection map / cube? (texture2d / texturecube)
    // refraction [https://docs.unrealengine.com/latest/INT/Engine/Rendering/Materials/HowTo/Refraction/index.html]

    // shininess              -\
    // roughness (float)      ----> Physically Based Rendering
    // metalic (float)        -/

    // specular color (vec3)
    // specular map (texture2d)

    // specular intensity (float)
    // specular power (float)

    // some opacity? (float / texture2d)

}
