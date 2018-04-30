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
import eu.matejkormuth.lpsim.ReferenceCounting;
import eu.matejkormuth.lpsim.UsageHint;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class BufferObject extends ReferenceCounting implements Disposable {

    private int vboId;

    public BufferObject() {
        vboId = GL15.glGenBuffers();
    }

    public void bind(BufferObjectTarget target) {
        GL15.glBindBuffer(target.getConstant(), vboId);
    }

    public void bindAsVbo() {
        this.bind(BufferObjectTarget.ARRAY_BUFFER);
    }

    public void bindAsArrayBuffer() {
        this.bind(BufferObjectTarget.ARRAY_BUFFER);
    }

    public void bindAsIbo() {
        this.bind(BufferObjectTarget.ELEMENT_ARRAY_BUFFER);
    }

    public void allocate(BufferObjectTarget target, long bytes, UsageHint usage) {
        GL15.glBufferData(target.getConstant(), bytes, usage.getConstant());
    }

    public void uploadData(BufferObjectTarget target, IntBuffer intBuffer, UsageHint usage) {
        GL15.glBufferData(target.getConstant(), intBuffer, usage.getConstant());
    }

    public void uploadData(BufferObjectTarget target, FloatBuffer floatBuffer, UsageHint usage) {
        GL15.glBufferData(target.getConstant(), floatBuffer, usage.getConstant());
    }

    public void uploadSubData(BufferObjectTarget target, long offset, FloatBuffer floatBuffer) {
        GL15.glBufferSubData(target.getConstant(), offset, floatBuffer);
    }

    @Override
    public void dispose() {
        GL15.glDeleteBuffers(vboId);
    }
}
