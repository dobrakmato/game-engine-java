/**
 * lpsim -
 * Copyright (c) 2015, Matej Kormuth <http://www.github.com/dobrakmato>
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 * <p>
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
import eu.matejkormuth.lpsim.math.Matrix4f;
import eu.matejkormuth.math.MathUtils;
import eu.matejkormuth.math.vectors.Vector2f;
import eu.matejkormuth.math.vectors.Vector3f;
import lombok.Getter;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Util;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public class SSAO {

    private static final int NOISE_TEXTURE_SIZE = 4;
    private static final int SAMPLES_COUNT = 64;
    private static final int NOISE_TEXTURE_SAMPLER_UNIT = 11;

    private static final Vector3f[] SAMPLES;
    private static final FloatBuffer NOISE_TEXTURE;

    @Getter
    private final FrameBuffer ssaoBuffer = new FrameBuffer();
    private final Program ssaoProgram = ShaderCollection.provideProgram("ssao");

    static {
        SAMPLES = new Vector3f[SAMPLES_COUNT];
        for (int i = 0; i < SAMPLES_COUNT; i++) {
            Vector3f v = new Vector3f(
                    (float) Math.random() * 2f - 1f,
                    (float) Math.random() * 2f - 1f,
                    (float) Math.random()
            );
            v = v.normalize();
            v = v.multiply((float) Math.random());

            // We want to distribute more kernel samples closer to the origin.
            float scale = (float) i / (float) SAMPLES_COUNT;
            scale = MathUtils.lerp(0.1f, 1.0f, scale * scale);

            v = v.multiply(scale);

            SAMPLES[i] = v;
        }

        NOISE_TEXTURE = BufferUtils.createFloatBuffer(4 * NOISE_TEXTURE_SIZE * NOISE_TEXTURE_SIZE);
        for (int i = 0; i < NOISE_TEXTURE_SIZE * NOISE_TEXTURE_SIZE; i++) {
            NOISE_TEXTURE.put((float) Math.random() * 2f - 1f);
            NOISE_TEXTURE.put((float) Math.random() * 2f - 1f);
            NOISE_TEXTURE.put(0.0f);
            NOISE_TEXTURE.put(1.0f);
        }
        NOISE_TEXTURE.flip();
    }

    @Getter
    private final Texture2D ssaoTexture;
    @Getter
    private final Texture2D noiseTexture;

    public SSAO(int width, int height) {
        ssaoProgram.use()
                .setUniform("c1", 1)
                .setUniform("texNoise", NOISE_TEXTURE_SAMPLER_UNIT)
                .setUniform("noiseScale", new Vector2f(width / (float) NOISE_TEXTURE_SIZE, height / (float) NOISE_TEXTURE_SIZE))
                .setUniform("depth", 9);
        for (int i = 0; i < SAMPLES_COUNT; i++) {
            ssaoProgram.setUniform("samples[" + i + "]", SAMPLES[i]);
        }

        // create texture for storing the ambient occlusion
        ssaoTexture = Texture2D.create();
        ssaoTexture.bind();
        ssaoTexture.setTag("SSAO Buffer Texture");
        ssaoTexture.setFilters(FilterMode.NEAREST, FilterMode.NEAREST);
        ssaoTexture.setImageDataFloat(GL_RGBA, GL_RGBA, width, height);

        ssaoBuffer.bind();
        ssaoBuffer.setTag("SSAO Generate Framebuffer");
        ssaoBuffer.attach(FrameBufferTarget.FRAMEBUFFER, ssaoTexture, GL_COLOR_ATTACHMENT0);
        ssaoBuffer.checkFramebuffer(FrameBufferTarget.FRAMEBUFFER);
        FrameBuffer.SCREEN.bind();

        // create noise texture
        Texture2D.activeSampler(11);
        noiseTexture = Texture2D.create();
        noiseTexture.bind();
        noiseTexture.setTag("SSAO Noise Texture");
        noiseTexture.setFilters(FilterMode.NEAREST, FilterMode.NEAREST);
        noiseTexture.setWraps(WrapMode.REPEAT, WrapMode.REPEAT);
        noiseTexture.setImageData(NOISE_TEXTURE, GL_RGBA, GL_RGBA32F, NOISE_TEXTURE_SIZE, NOISE_TEXTURE_SIZE);

        Util.checkGLError();
    }

    public void passSSAO(Matrix4f projection, Matrix4f view) {
        ssaoBuffer.bindForWriting();
        glClear(GL_COLOR_BUFFER_BIT);
        ssaoProgram.use()
                .setUniform("projection", projection)
                .setUniform("view", view);
    }

    public void passBlur() {
        ssaoBuffer.bindForReading();
    }
}
