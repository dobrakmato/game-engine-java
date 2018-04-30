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
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.Util;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

@Slf4j
public class TextureCube implements Disposable, Tagable {

    @Getter
    private int id;

    private int width = -1;
    private int height = -1;
    private int internalFormat = -1;

    private static int bound = 0; // WARN: Multithreading.

    @Getter
    private String tag;

    private TextureCube(int id) {
        this.id = id;
    }

    /**
     * Creates textures in batches and stores them in pool.
     */
    private static class Pool {
        // Number of textures created in one batch.
        private final int batchSize = 4;
        // Free (new) textures.
        private final Queue<TextureCube> free = new ArrayBlockingQueue<>(batchSize, true);

        private Pool() {
            batch();
        }

        private void batch() {
            IntBuffer textures = BufferUtils.createIntBuffer(batchSize);
            GL11.glGenTextures(textures);
            for (int i = 0; i < batchSize; i++) {
                free.add(new TextureCube(textures.get()));
            }
            log.info(" TextureCube/Pool created {} textures.", batchSize);
        }

        private TextureCube take() {
            if (free.isEmpty()) {
                this.batch();
            }
            return free.poll();
        }
    }

    // pool instance
    static Pool pool = new Pool();

    public static TextureCube create() {
        return pool.take();
    }

    public void bind() {
        GL11.glBindTexture(GL_TEXTURE_CUBE_MAP, id);
        bound = id;
    }

    public boolean hasTag() {
        return tag != null;
    }

    public void setTag(String tag) {
        this.tag = tag;

        String glTag = tag;
        if (glTag.length() > 254) {
            int start = glTag.length() - 254;
            glTag = glTag.substring(start);
        }

        GL43.glObjectLabel(GL_TEXTURE, id, glTag);
    }

    private void ensureBound(String str) {
        if (bound != id) {
            log.warn("Texture2D not bound when {} was called!", str);
            bind();
        }
    }


    public void generateMipmaps() {
        ensureBound("generateMipmaps");

        GL30.glGenerateMipmap(GL_TEXTURE_CUBE_MAP);
    }


    public void setWraps(WrapMode horizontal, WrapMode vertical) {
        ensureBound("setWraps");

        GL11.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, horizontal.getGLConstant());
        GL11.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, vertical.getGLConstant());
    }

    public void setFilters(FilterMode minFilter, FilterMode magFilter) {
        if (magFilter.needsMipmaps()) {
            throw new IllegalArgumentException("Mag filter cannot use mipmaps!");
        }

        ensureBound("setFilters");

        GL11.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, minFilter.getGLConstant());
        GL11.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, magFilter.getGLConstant());
    }

    private void verifySameParameters(int width, int height, int internalFormat) {
        if (this.width != -1 || this.height != -1 || this.internalFormat != -1) {
            if (this.width != width || this.height != height || this.internalFormat != internalFormat) {
                throw new IllegalArgumentException("All faces must have the same width, height and internal format!");
            }
        }

        this.width = width;
        this.height = height;
        this.internalFormat = internalFormat;
    }

    public void setPositiveXImageData(int width, int height, int format, int internalFormat, ByteBuffer imageData) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, internalFormat, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, 0, 0, width, height, format, GL11.GL_UNSIGNED_BYTE, imageData);
    }

    public void setPositiveYImageData(int width, int height, int format, int internalFormat, ByteBuffer imageData) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, internalFormat, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, 0, 0, width, height, format, GL11.GL_UNSIGNED_BYTE, imageData);
    }

    public void setPositiveZImageData(int width, int height, int format, int internalFormat, ByteBuffer imageData) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, internalFormat, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, 0, 0, width, height, format, GL11.GL_UNSIGNED_BYTE, imageData);
    }

    public void setNegativeXImageData(int width, int height, int format, int internalFormat, ByteBuffer imageData) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, internalFormat, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, 0, 0, width, height, format, GL11.GL_UNSIGNED_BYTE, imageData);
    }

    public void setNegativeYImageData(int width, int height, int format, int internalFormat, ByteBuffer imageData) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, internalFormat, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, 0, 0, width, height, format, GL11.GL_UNSIGNED_BYTE, imageData);
    }

    public void setNegativeZImageData(int width, int height, int format, int internalFormat, ByteBuffer imageData) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, internalFormat, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, 0, 0, width, height, format, GL11.GL_UNSIGNED_BYTE, imageData);
    }


    public void setPositiveXImageData(int width, int height, int format, int internalFormat, FloatBuffer imageData) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, (FloatBuffer) null);
        GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, 0, 0, width, height, format, GL11.GL_FLOAT, imageData);
    }

    public void setPositiveYImageData(int width, int height, int format, int internalFormat, FloatBuffer imageData) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, (FloatBuffer) null);
        GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, 0, 0, width, height, format, GL11.GL_FLOAT, imageData);
    }

    public void setPositiveZImageData(int width, int height, int format, int internalFormat, FloatBuffer imageData) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, (FloatBuffer) null);
        GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, 0, 0, width, height, format, GL11.GL_FLOAT, imageData);
    }

    public void setNegativeXImageData(int width, int height, int format, int internalFormat, FloatBuffer imageData) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, (FloatBuffer) null);
        GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, 0, 0, width, height, format, GL11.GL_FLOAT, imageData);
    }

    public void setNegativeYImageData(int width, int height, int format, int internalFormat, FloatBuffer imageData) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, (FloatBuffer) null);
        GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, 0, 0, width, height, format, GL11.GL_FLOAT, imageData);
    }

    public void setNegativeZImageData(int width, int height, int format, int internalFormat, FloatBuffer imageData) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, (FloatBuffer) null);
        GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, 0, 0, width, height, format, GL11.GL_FLOAT, imageData);
    }

    public void createPositiveXImageData(int width, int height, int format, int internalFormat) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, (FloatBuffer) null);
    }

    public void createPositiveYImageData(int width, int height, int format, int internalFormat) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, (FloatBuffer) null);
    }

    public void createPositiveZImageData(int width, int height, int format, int internalFormat) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, (FloatBuffer) null);
    }

    public void createNegativeXImageData(int width, int height, int format, int internalFormat) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, (FloatBuffer) null);
    }

    public void createNegativeYImageData(int width, int height, int format, int internalFormat) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, (FloatBuffer) null);
    }

    public void createNegativeZImageData(int width, int height, int format, int internalFormat) {
        verifySameParameters(width, height, internalFormat);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, (FloatBuffer) null);
    }

    @Deprecated
    public void setImageData(int width, int height, int format, int internalFormat,
                             ByteBuffer posX, ByteBuffer posY, ByteBuffer posZ,
                             ByteBuffer negX, ByteBuffer negY, ByteBuffer negZ) {

        verifySameParameters(width, height, internalFormat);
        Util.checkGLError();
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, internalFormat, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, posX);
        Util.checkGLError();
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, internalFormat, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, posY);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, internalFormat, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, posZ);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, internalFormat, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, negX);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, internalFormat, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, negY);
        GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, internalFormat, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, negZ);
        Util.checkGLError();
    }

    @Override
    public void dispose() {
        if (id != -1) {
            GL11.glDeleteTextures(id);
            id = -1;
        }
    }
}
