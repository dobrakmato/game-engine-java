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
import eu.matejkormuth.lpsim.gl.VAO;
import lombok.Getter;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;

/**
 * Represents renderable geometry in GPU memory.
 * Model uses this class internally.
 */
public class Mesh extends ReferenceCounting implements Disposable {

    private VAO vao;
    @Getter
    private BufferObject vbo;
    @Getter
    private BufferObject ibo;
    @Getter
    private int indicesCount = 0;

    public Mesh(Geometry geometry, InterleavedVertexLayout layout) {
        this.init(geometry, layout);
    }

    private void init(Geometry geometry, InterleavedVertexLayout layout) {
        vao = new VAO();
        vbo = new BufferObject();
        ibo = new BufferObject();
        vao.bind();
        ibo.bindAsIbo();
        vbo.bindAsVbo();
        layout.applyToBoundVAO();

        VertexBufferBuilder builder = VertexBufferBuilder.of(layout, geometry);
        indicesCount = geometry.getIndices().length;

        ibo.uploadData(BufferObjectTarget.ELEMENT_ARRAY_BUFFER, geometry.createIndicesBuffer(), UsageHint.STATIC_DRAW);
        vbo.uploadData(BufferObjectTarget.ARRAY_BUFFER, builder.create(geometry.getPositions().length), UsageHint.STATIC_DRAW);
    }

    public void drawElements() {
        vao.bind();
        GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_INT, 0);
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
    }

    public void drawElementsInstanced(int instanceCount) {
        vao.bind();
        GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_INT, 0, instanceCount);
    }
}
