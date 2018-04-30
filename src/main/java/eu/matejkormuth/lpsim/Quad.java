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

import eu.matejkormuth.lpsim.gl.*;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@Slf4j
public class Quad implements Disposable {

    public static final FloatBuffer vertexBuffer = (FloatBuffer) BufferUtils.createFloatBuffer(16).put(new float[]{
            -1, -1, 0, 1,   //0
            -1, 1, 1, 1,    //1
            1, -1, 0, 0,    //2
            1, 1, 1, 0     //3
    }).flip();
    public static final IntBuffer indices = (IntBuffer) BufferUtils.createIntBuffer(6).put(new int[]{
            0, 1, 2, // triangle 1
            1, 3, 2  // triangle 2
    }).flip();
    private static final InterleavedVertexLayout layout = InterleavedVertexLayout
            .builder()
            .attribute("position", InterleavedVertexLayout.AttributeType.VEC2)
            .attribute("texCoord", InterleavedVertexLayout.AttributeType.VEC2)
            .build();

    private VAO vao;
    private BufferObject vbo;
    private BufferObject ibo;
    private Program program = ShaderCollection.provideProgram("quad");

    public Quad() {
        initRendering();
    }

    private void initRendering() {
        vao = new VAO();
        vbo = new BufferObject();
        ibo = new BufferObject();
        vao.bind();
        vbo.bindAsVbo();
        ibo.bindAsIbo();
        layout.applyToBoundVAO();

        vbo.uploadData(BufferObjectTarget.ARRAY_BUFFER, vertexBuffer, UsageHint.STATIC_DRAW);
        ibo.uploadData(BufferObjectTarget.ELEMENT_ARRAY_BUFFER, indices, UsageHint.STATIC_DRAW);

        vao.unbind();

        program.use().setUniform("texMap", 0);
    }

    public void render(Texture2D texture) {
        vao.bind();
        Texture2D.activeSampler(0);
        texture.bind();

        program.use();
        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
    }

    @Override
    public void dispose() {
        vbo.dispose();
        vao.dispose();
        program.dispose();
    }
}
