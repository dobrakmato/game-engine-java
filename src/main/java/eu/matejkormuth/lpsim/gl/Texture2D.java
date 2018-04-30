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

import eu.matejkormuth.bf.compression.BFInputStream;
import eu.matejkormuth.bf.image.ImageFile;
import eu.matejkormuth.lpsim.Application;
import eu.matejkormuth.lpsim.Disposable;
import eu.matejkormuth.lpsim.Tagable;
import eu.matejkormuth.lpsim.content.Content;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_RG;

@Slf4j
public class Texture2D implements Disposable, Tagable {

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

    @Getter
    private String tag;

    /**
     * Creates textures in batches and stores them in pool.
     */
    private static class Pool {
        // Number of textures created in one batch.
        private final int batchSize = 32;
        // Free (new) textures.
        private final Queue<Texture2D> free = new ArrayBlockingQueue<>(batchSize, true);
        private final List<WeakReference<Texture2D>> all = new ArrayList<>();

        private Pool() {
            batch();
        }

        private void batch() {
            IntBuffer textures = BufferUtils.createIntBuffer(batchSize);
            GL11.glGenTextures(textures);
            for (int i = 0; i < batchSize; i++) {
                Texture2D tex = new Texture2D(textures.get());
                free.add(tex);
                all.add(new WeakReference<>(tex));
            }
            log.info(" Texture2D/Pool created {} textures.", batchSize);
        }

        private Texture2D take() {
            if (free.isEmpty()) {
                this.batch();
            }
            return free.poll();
        }
    }

    public static class Util {
        public static final Texture2D BLACK;
        public static final Texture2D WHITE;
        public static final Texture2D ERROR;
        public static final Texture2D FLAT_NORMAL;

        static {
            try {
                Texture2D blackTex = Texture2D.create();
                ImageFile.loadIntoTexture(new BFInputStream(Content.getContent().openRead("textures", "black.bif")), blackTex);
                blackTex.bind();
                blackTex.setTag("TextureUtil/BLACK");
                //GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 0.0f);
                BLACK = blackTex;

                Texture2D whiteTex = Texture2D.create();
                ImageFile.loadIntoTexture(new BFInputStream(Content.getContent().openRead("textures", "white.bif")), whiteTex);
                whiteTex.bind();
                whiteTex.setTag("TextureUtil/WHITE");
                //GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 0.0f);
                WHITE = whiteTex;

                Texture2D errorTex = Texture2D.create();
                ImageFile.loadIntoTexture(new BFInputStream(Content.getContent().openRead("textures", "error.bif")), errorTex);
                errorTex.bind();
                errorTex.setTag("TextureUtil/ERROR");
                //GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 0.0f);
                ERROR = errorTex;

                Texture2D flatNormalTex = Texture2D.create();
                ImageFile.loadIntoTexture(new BFInputStream(Content.getContent().openRead("textures", "flat_normal.bif")), flatNormalTex);
                flatNormalTex.bind();
                flatNormalTex.setTag("TextureUtil/FLAT_NORMAL");
                //GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 0.0f);
                FLAT_NORMAL = flatNormalTex;
            } catch (IOException e) {
                throw new RuntimeException("System files corrupted, cant continue!", e);
            }
        }
    }

    // pool instance
    static Pool pool = new Pool();

    public static Texture2D create() {
        return pool.take();
    }

    public static List<Texture2D> find(@Nonnull String tag) {
        ArrayList<Texture2D> results = new ArrayList<>();
        for (WeakReference<Texture2D> ref : pool.all) {
            if (ref.get() != null) {
                if (tag.equals(ref.get().getTag())) {
                    results.add(ref.get());
                }
            }
        }
        return results;
    }

    public static void disposeAll() {
        int textures = 0;
        long bytes = 0;
        for (WeakReference<Texture2D> ref : pool.all) {
            if (ref.get() != null) {
                Texture2D tex = ref.get();
                textures++;
                int channels = 1;
                int bytesPerPixelPerChannel = 1;
                switch (tex.format) {
                    case GL_RG:
                        channels = 2;
                        break;
                    case GL_RGB:
                        channels = 3;
                        break;
                    case GL_RGBA:
                        channels = 4;
                        break;
                }
                tex.dispose();
                long bts = (tex.width * tex.height) * channels * bytesPerPixelPerChannel;
                bytes += bts;
            }
        }
        log.info("Freed {} textures ({} MB)", textures, (bytes / 1_000_000));
    }

    private static int bound = 0; // WARN: Multithreading.
    static int activeTexture = 0; // Todo: Warn: multithreading

    public Texture2D() {
        id = GL11.glGenTextures();
    }

    private Texture2D(int id) {
        this.id = id;
    }

    public void bind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        bound = this.id;
    }

    public void unbind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
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

    public void setTag(String tag, String label) {
        this.tag = tag;

        if (label.length() > 254) {
            int start = label.length() - 254;
            label = label.substring(start);
        }

        GL43.glObjectLabel(GL_TEXTURE, id, label);
    }

    public static void activeSampler(int sampler) {
        if (sampler != activeTexture) {
            activeTexture = sampler;
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + sampler);
        }
    }

    public void generateMipmaps() {
        ensureBound("generateMipmaps");

        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
    }

    private void ensureBound(String str) {
        if (bound != id) {
            log.warn("Texture2D not bound when {} was called!", str);
            bind();
        }
    }

    public void enableMaxAF() {
        ensureBound("enableMaxAF");

        if (Application.get().getAnisoMax() != 0.0f) {
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, Application.get().getAnisoMax());
        }
    }

    public void setWraps(WrapMode horizontal, WrapMode vertical) {
        ensureBound("setWraps");

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, horizontal.getGLConstant());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, vertical.getGLConstant());
    }

    public void setFilters(FilterMode minFilter, FilterMode magFilter) {
        if (magFilter.needsMipmaps()) {
            throw new IllegalArgumentException("Mag filter cannot use mipmaps!");
        }

        ensureBound("setFilters");

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter.getGLConstant());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter.getGLConstant());
    }

    public void setImageData(ByteBuffer data, int format, int internalFormat, int width, int height) {
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

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, height, format, GL11.GL_UNSIGNED_BYTE, data);
        //Util.checkGLError();
    }

    public void setImageData(FloatBuffer data, int format, int internalFormat, int width, int height) {
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

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, data);
    }

    public void setImageDataFloat(int internalFormat, int format, int width, int height) {
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

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, (FloatBuffer) null);
    }

    public void setImageDataUnsignedByte(int internalFormat, int format, int width, int height) {
        setImageDataUnsignedByte(internalFormat, format, width, height, GL11.GL_UNSIGNED_BYTE);
    }

    public void setImageDataUnsignedByte(int internalFormat, int format, int width, int height, int type) {
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

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, (IntBuffer) null);
    }

    // broken
    public ByteBuffer getImageData(int bitsPerPixel) {
        ensureBound("acquire image data");

        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * bitsPerPixel / 8); // todo: fix magic numer 4 <= because RGBA bytes
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, this.format, GL11.GL_UNSIGNED_BYTE, buffer);

        return buffer;
    }

    @Override
    public void dispose() {
        GL11.glDeleteTextures(id);
        id = -1;
    }
}
