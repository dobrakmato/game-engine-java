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
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Class that contains shadow map for variance shadow mapping.
 */
public class VSM implements Disposable {

    private final FrameBuffer frameBuffer;
    private final FrameBuffer tempFrameBuffer;

    @Getter
    private final int size;
    private final int samples = 2;

    @Getter
    private Texture2D shadowMap;
    @Getter
    private Texture2D tempMap;

    private int rbo;

    @Getter
    private final Program program = ShaderCollection.provideProgram("vsm");

    public VSM(int size) {
        this.size = size;

        this.frameBuffer = new FrameBuffer();
        this.tempFrameBuffer = new FrameBuffer();
        this.initialize();
        this.initializeTemp();
    }

    private void initializeTemp() {
        tempFrameBuffer.bind();
        tempFrameBuffer.setTag("VSM FBO (temp) #" + this.toString());

        tempMap = Texture2D.create();
        tempMap.bind();
        tempMap.setTag("VSM Map (temp) #" + this.toString());
        tempMap.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        tempMap.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
        tempMap.setImageDataFloat(GL_RG32F, GL_RGBA, size, size);

        tempFrameBuffer.attach(FrameBufferTarget.FRAMEBUFFER, tempMap, GL_COLOR_ATTACHMENT0);
        tempFrameBuffer.checkFramebuffer(FrameBufferTarget.FRAMEBUFFER);
    }

    private void initialize() {
        frameBuffer.bind();
        frameBuffer.setTag("VSM FBO #" + this.toString());

        shadowMap = Texture2D.create();
        shadowMap.bind();
        shadowMap.setTag("VSM Map #" + this.toString());
        shadowMap.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        shadowMap.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);

        if (Application.get().getAnisoMax() != 0.0f) {
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, Application.get().getAnisoMax());
        }

        shadowMap.setImageDataFloat(GL_RG32F, GL_RGBA, size, size);
        frameBuffer.attach(FrameBufferTarget.FRAMEBUFFER, shadowMap, GL_COLOR_ATTACHMENT0);


        int rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, size, size);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rbo);

        frameBuffer.checkFramebuffer(FrameBufferTarget.FRAMEBUFFER);
    }

    public void bindForWriting() {
        frameBuffer.bindForWriting();

        GL11.glClearColor(255, 255, 255, 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the shadow map
        Application.get().viewport(size, size); // prepare for rendering

        program.use();
    }

    public void bindTempForWriting() {
        tempFrameBuffer.bindForWriting();
    }

    public void bindForReading() {
        frameBuffer.bindForReading();
    }

    @Override
    public void dispose() {
        shadowMap.dispose();
        glDeleteRenderbuffers(rbo);
        frameBuffer.dispose();
    }
}
