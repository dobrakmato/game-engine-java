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
package eu.matejkormuth.lpsim.gl;

import eu.matejkormuth.lpsim.Disposable;
import eu.matejkormuth.lpsim.Tagable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.*;

import static org.lwjgl.opengl.GL11.*;

/**
 * @since OpenGL 3.0
 */
@Slf4j
public class VAO implements Disposable, Tagable {

    private static int currentVao = -1;

    private int vaoId;
    private int vertexAttributeCount = 0;
    private int currentOffset = 0;

    @Getter
    private String tag;

    public VAO() {
        vaoId = GL30.glGenVertexArrays();
    }

    // Size = amount of types (3 for vec3)
    // Type = GL_FLOAT or somethings.
    // VertexSize = size of whole vertex
    public void nextAttributePointer(int index, int size, AttributeType type, int stride, int offset) {
        GL20.glEnableVertexAttribArray(index);
        GL20.glVertexAttribPointer(index, size, type.getConstant(), false, stride, offset);

        //currentOffset += size * type.getBytes();
        //vertexAttributeCount++;
    }

    public void bind() {
        if (currentVao == vaoId) {
            //log.warn("Tried to bind the same vao ({}) that is currently bound ({})!", vaoId, currentVao);
            return;
        }

        GL30.glBindVertexArray(vaoId);
        currentVao = vaoId;
    }

    public void unbind() {
        GL30.glBindVertexArray(0);
        currentVao = 0;
    }

    public boolean hasTag() {
        return tag != null;
    }

    public void setTag(String tag) {
        this.tag = tag;

        GL43.glObjectLabel(GL_TEXTURE, vaoId, tag);
        Util.checkGLError();
    }

    @Override
    public void dispose() {
        GL30.glDeleteVertexArrays(vaoId);
    }

    public enum AttributeType {
        FLOAT(Float.SIZE, GL11.GL_FLOAT);

        private final int bytes;
        private final int constant;

        AttributeType(int bytes, int constant) {
            this.bytes = bytes;
            this.constant = constant;
        }

        public int getBytes() {
            return bytes;
        }

        public int getConstant() {
            return constant;
        }
    }
}
