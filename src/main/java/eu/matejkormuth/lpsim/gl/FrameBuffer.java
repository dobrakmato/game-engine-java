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
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.Util;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

@Slf4j
public class FrameBuffer implements Disposable, Tagable {

    public static final FrameBuffer SCREEN = new FrameBuffer() {
        @Override
        public void bind(FrameBufferTarget target) {
            //if (FrameBuffer.currentFbo == 0) {
            //    return;
            //}
            GL30.glBindFramebuffer(target.getConstant(), 0);
            //FrameBuffer.currentFbo = 0;
        }

        @Override
        public void bind() {
            this.bind(FrameBufferTarget.FRAMEBUFFER);
        }
    };

    private static int currentFbo = 0;
    private int fboId;

    @Getter
    private String tag;

    public void setTag(String tag) {
        this.tag = tag;
        GL43.glObjectLabel(GL_FRAMEBUFFER, this.fboId, tag);
        Util.checkGLError();
    }

    public FrameBuffer() {
        fboId = GL30.glGenFramebuffers();
    }

    public void bind(FrameBufferTarget target) {
        //if (FrameBuffer.currentFbo == fboId) {
        //    return;
        //}

        GL30.glBindFramebuffer(target.getConstant(), fboId);
        //FrameBuffer.currentFbo = fboId;
    }

    /**
     * Same as calling <code>bind(FrameBufferTarget.FRAMEBUFFER)</code>.
     */
    public void bind() {
        bind(FrameBufferTarget.FRAMEBUFFER);
    }

    public void bindForWriting() {
        bind(FrameBufferTarget.DRAW);
    }

    public void bindForReading() {
        bind(FrameBufferTarget.READ);
    }

    public void attach(FrameBufferTarget target, int attachment, int textureId, int textureTarget) {
        attach(target, attachment, textureId, textureTarget, 0);
    }

    public void attach(FrameBufferTarget target, int attachment, int textureId, int textureTarget, int level) {
        GL30.glFramebufferTexture2D(target.getConstant(), attachment, textureTarget, textureId, level);
    }

    public void attach(FrameBufferTarget target, Texture2D texture, int attachment) {
        GL30.glFramebufferTexture2D(target.getConstant(), attachment, GL_TEXTURE_2D, texture.getId(), 0);
        //GL32.glFramebufferTexture(target.getConstant(), attachment, texture.getId(), 0);
    }

    public void attachMultisample(FrameBufferTarget target, int textureId, int attachment) {
        GL30.glFramebufferTexture2D(target.getConstant(), attachment, GL_TEXTURE_2D_MULTISAMPLE, textureId, 0);
        //GL32.glFramebufferTexture(target.getConstant(), attachment, texture.getId(), 0);
    }

    public void attachLayer(FrameBufferTarget target, Texture2DArray texture, int attachment, int layer) {
        GL30.glFramebufferTextureLayer(target.getConstant(), attachment, texture.getId(), 0, layer);
    }

    public void attachRenderBuffer(FrameBufferTarget target, RenderBuffer depthTexture, int attachment) {
        glFramebufferRenderbuffer(target.getConstant(), attachment, GL_RENDERBUFFER, depthTexture.getId());
    }

    public void drawBuffer(int mode) {
        GL11.glDrawBuffer(mode);
    }

    public void readBuffer(int mode) {
        GL11.glReadBuffer(mode);
    }

    public void checkFramebuffer(FrameBufferTarget target) {
        if (GL30.glCheckFramebufferStatus(target.getConstant()) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer is broken! (" + GL30.glCheckFramebufferStatus(target.getConstant()) + ")");
        }
    }

    @Override
    public void dispose() {
        GL30.glDeleteFramebuffers(fboId);
    }


}
