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
import lombok.Getter;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.Util;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

public class ShadowMap implements Disposable {

    @Getter
    private Program program = ShaderCollection.provideProgram("shadowMap");
    private FrameBuffer frameBuffer;
    @Getter
    private Texture2D texture;
    @Getter
    private int size;

    public ShadowMap() {
        this(2048);
    }

    public ShadowMap(int size) {
        this.size = size;
        initRendering();
    }

    private void initRendering() {
        Util.checkGLError();
        frameBuffer = new FrameBuffer();
        texture = Texture2D.create();

        frameBuffer.bind(FrameBufferTarget.FRAMEBUFFER);
        //frameBuffer.drawBuffer(GL11.GL_NONE);
        //frameBuffer.readBuffer(GL11.GL_NONE);
        frameBuffer.setTag("ShadowMap #" + this.toString());

        texture.bind();
        texture.setImageDataFloat(GL14.GL_DEPTH_COMPONENT16, GL11.GL_DEPTH_COMPONENT, size, size);
        texture.setFilters(FilterMode.NEAREST, FilterMode.LINEAR);
        texture.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
        texture.setTag("ShadowMap #" + this.toString());

        GL11.glTexParameteri(GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_FUNC, GL11.GL_LEQUAL);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL14.GL_COMPARE_R_TO_TEXTURE); // We want

        frameBuffer.attach(FrameBufferTarget.FRAMEBUFFER, texture, GL30.GL_DEPTH_ATTACHMENT);
        frameBuffer.checkFramebuffer(FrameBufferTarget.FRAMEBUFFER);

        // Unbind.
        FrameBuffer.SCREEN.bind(FrameBufferTarget.FRAMEBUFFER);
    }

    public void bindForWriting() {
        frameBuffer.bindForWriting();

        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        //glViewport(0, 0, size, size);
        Application.get().viewport(size, size);

        program.use();
    }

    public void bindForReading() {
        frameBuffer.bindForReading();
    }

    @Override
    public void dispose() {
        frameBuffer.dispose();
        texture.dispose();
    }


}
