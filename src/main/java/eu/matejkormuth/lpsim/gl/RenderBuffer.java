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

import eu.matejkormuth.lpsim.ReferenceCounting;
import eu.matejkormuth.lpsim.Tagable;
import lombok.Getter;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;

import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.glRenderbufferStorage;

public class RenderBuffer extends ReferenceCounting implements Tagable {

    @Getter
    private int id;

    @Getter
    private String tag;

    public RenderBuffer() {
        this.id = GL30.glGenRenderbuffers();
    }

    public void bind() {
        GL30.glBindRenderbuffer(GL_RENDERBUFFER, id);
    }

    public void createStorage(int internalFormat, int width, int height) {
        glRenderbufferStorage(GL_RENDERBUFFER, internalFormat, width, height);
    }

    @Override
    public void dispose() {
        GL30.glDeleteRenderbuffers(id);
        id = -1;
    }

    @Override
    public void setTag(String tag) {
        this.tag = tag;

        String glTag = tag;
        if (glTag.length() > 254) {
            int start = glTag.length() - 254;
            glTag = glTag.substring(start);
        }

        GL43.glObjectLabel(GL_RENDERBUFFER, id, glTag);
    }
}
