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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

@Slf4j
public class Texture2DArray implements Disposable {

    @Getter
    private int id;

    // cached from last setImageData call.
    @Getter
    private int format;
    @Getter
    private int internalFormat;
    @Getter
    private int width;
    @Getter
    private int height;


    private static int bound = 0; // WARN: Multithreading.

    public Texture2DArray() {
        id = GL11.glGenTextures();
    }

    public void bind() {
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, id);
        bound = this.id;
    }

    public void unbind() {
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
    }

    public static void activeSampler(int sampler) {
        // Active textures are shader trough all texture targets / types.
        if (sampler != Texture2D.activeTexture) {
            Texture2D.activeTexture = sampler;
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + sampler);
        }
    }

    private void ensureBound(String str) {
        if (bound != id) {
            log.warn("Texture2DArray not bound when {} was called!", str);
            bind();
        }
    }

    public void setWraps(WrapMode horizontal, WrapMode vertical) {
        ensureBound("setWraps");

        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S, horizontal.getGLConstant());
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T, vertical.getGLConstant());
    }

    public void setFilters(FilterMode minFilter, FilterMode magFilter) {
        if (magFilter.needsMipmaps()) {
            throw new IllegalArgumentException("Mag filter cannot use mipmaps!");
        }

        ensureBound("setFilters");

        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, minFilter.getGLConstant());
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, magFilter.getGLConstant());
    }

    public void setImageData(int internalFormat, int format, int width, int height, int layers) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be greater then zero");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be greater then zero");
        }
        ensureBound("set image data");

        this.internalFormat = internalFormat;
        this.format = format;
        this.width = width;
        this.height = height;

        // glTexImage3D(int target, int level, int internalFormat, int width, int height, int depth, int border, int format, int type, ByteBuffer pixels) {
        GL12.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, internalFormat, width, height, layers, 0, format, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
    }

    @Override
    public void dispose() {
        GL11.glDeleteTextures(id);
        id = -1;
    }
}
