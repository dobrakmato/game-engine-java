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

import eu.matejkormuth.lpsim.gl.BufferObject;
import eu.matejkormuth.lpsim.gl.BufferObjectTarget;
import eu.matejkormuth.lpsim.gl.Program;
import eu.matejkormuth.lpsim.gl.VAO;
import org.lwjgl.opengl.GL11;

public class Water extends WorldObject {

    private InterleavedVertexLayout layout;
    private Program program;

    private VAO vao;
    private BufferObject vbo;
    private BufferObject ibo;
    private int indicesCount = 0;

    public Water() {
        layout = InterleavedVertexLayout.builder()
                .attribute("position", InterleavedVertexLayout.AttributeType.VEC3) // vec3 (x, y, z)
                .build();

        initRender();
    }

    private void initRender() {
        program = ShaderCollection.provideProgram("water2");
        vao = new VAO();
        vbo = new BufferObject();
        ibo = new BufferObject();
        vao.bind();
        ibo.bindAsIbo();
        vbo.bindAsVbo();
        layout.applyToBoundVAO();

        Geometry plane = Geometry.plane(768);
        VertexBufferBuilder bufferBuilder = VertexBufferBuilder.of(layout, plane);
        indicesCount = plane.getIndices().length;

        ibo.uploadData(BufferObjectTarget.ELEMENT_ARRAY_BUFFER, plane.createIndicesBuffer(), UsageHint.STATIC_DRAW);
        vbo.uploadData(BufferObjectTarget.ARRAY_BUFFER, bufferBuilder.create(plane.getPositions().length), UsageHint.STATIC_DRAW);
    }

    @Override
    public void render(Camera camera) {
        vao.bind();
        GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_INT, 0);
    }

    @Override
    public void update(float deltaTime) {

    }

    @Override
    public LightPrograms getLightPrograms() {
        return null;
    }

    @Override
    public void dispose() {
        if (vao != null) {
            vao.dispose();
        }
        if (vbo != null) {
            vbo.dispose();
        }
        if (ibo != null) {
            ibo.dispose();
        }
        program = null;
    }
}
