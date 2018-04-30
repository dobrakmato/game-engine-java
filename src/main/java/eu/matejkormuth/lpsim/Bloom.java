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
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.Util;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_RGB16F;
import static org.lwjgl.opengl.GL30.glBlitFramebuffer;

@Slf4j
public class Bloom {

    private static final int BLOOM_INTERNAL_FORMAT = GL_RGB16F; //GL_RGB16F;

    private final FrameBuffer[] fbos;
    private final FrameBuffer[] tempFbos;

    private final Texture2D[] colorTextures;
    private final Texture2D[] tempColorTextures;

    private final int[] dividers;
    private final int[] widths;
    private final int[] heights;
    private final float[] bloomWeights;

    private final float[] offsetsX;
    private final float[] offsetsY;

    private final Mesh quadMesh;

    private Program bloomLight = ShaderCollection.provideProgram("bloomLight");
    private Program bloomCombine = ShaderCollection.provideProgram("bloomCombine");
    private Program blur9x9 = ShaderCollection.provideProgram("blur9x9");

    public Bloom(int blurs, Mesh quadMesh) {
        this.quadMesh = quadMesh;

        fbos = new FrameBuffer[blurs];
        tempFbos = new FrameBuffer[blurs];
        colorTextures = new Texture2D[blurs];
        tempColorTextures = new Texture2D[blurs];
        dividers = new int[blurs];
        widths = new int[blurs];
        heights = new int[blurs];
        bloomWeights = new float[blurs];

        offsetsX = new float[blurs];
        offsetsY = new float[blurs];

        int fullWidth = World.CANVAS_WIDTH;
        int fullHeight = World.CANVAS_HEIGHT;

        for (int i = 0; i < blurs; i++) {
            int divider = (int) (Math.pow(2, i + 1));

            dividers[i] = divider;
            bloomWeights[i] = (10f - i) / 35f; //1.0f / divider;

            int width = fullWidth / divider;
            int height = fullHeight / divider;

            float diffX = (float) fullWidth - (divider * width);
            float diffY = (float) fullHeight - (divider * height);

            float offsetX = (diffX / 2) / fullWidth;
            float offsetY = (diffY / 2) / fullHeight;

            widths[i] = width;
            heights[i] = height;
            offsetsX[i] = offsetX;
            offsetsY[i] = offsetY;

            // Create FBO
            fbos[i] = new FrameBuffer();
            fbos[i].bind();
            fbos[i].setTag("Bloom " + i);
            colorTextures[i] = Texture2D.create();
            colorTextures[i].bind();
            colorTextures[i].setTag("Bloom " + i);
            colorTextures[i].setImageDataUnsignedByte(BLOOM_INTERNAL_FORMAT, GL_RGB, width, height);
            colorTextures[i].setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
            colorTextures[i].setWraps(WrapMode.CLAMP_TO_BORDER, WrapMode.CLAMP_TO_BORDER);
            fbos[i].attach(FrameBufferTarget.FRAMEBUFFER, colorTextures[i], GL30.GL_COLOR_ATTACHMENT0);
            fbos[i].checkFramebuffer(FrameBufferTarget.FRAMEBUFFER);

            // Create Temp FBO
            tempFbos[i] = new FrameBuffer();
            tempFbos[i].bind();
            tempFbos[i].setTag("Bloom " + i + " (temp)");
            tempColorTextures[i] = Texture2D.create();
            tempColorTextures[i].bind();
            tempColorTextures[i].setTag("Bloom " + i + " (temp)");
            tempColorTextures[i].setImageDataUnsignedByte(BLOOM_INTERNAL_FORMAT, GL_RGB, width, height);
            tempColorTextures[i].setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
            tempColorTextures[i].setWraps(WrapMode.CLAMP_TO_BORDER, WrapMode.CLAMP_TO_BORDER);
            tempFbos[i].attach(FrameBufferTarget.FRAMEBUFFER, tempColorTextures[i], GL30.GL_COLOR_ATTACHMENT0);
            tempFbos[i].checkFramebuffer(FrameBufferTarget.FRAMEBUFFER);

            log.info("Created Bloom{} (1/{}; {}x{}) FBOs!", i + 1, divider, width, height);
        }

        Util.checkGLError();

        int[] samplers = new int[blurs];
        for (int i = 0; i < blurs; i++) samplers[i] = i;

        bloomCombine.use()
                .setUniform("textures", samplers)
                .setUniform("offsetsX", offsetsX)
                .setUniform("offsetsY", offsetsY)
                .setUniform("weights", bloomWeights);
    }

    public void passOneLightFilter(float exposure) {
        bloomLight.use().setUniform("exposure", exposure);
        for (int i = 0; i < fbos.length; i++) {
            if (i != 0) {
                //colorTextures[i - 1].bind();
                fbos[i - 1].bindForReading();
                fbos[i].bindForWriting();
                glBlitFramebuffer(0, 0, widths[i - 1], heights[i - 1], 0, 0, widths[i], heights[i], GL_COLOR_BUFFER_BIT, GL_LINEAR);
            } else {
                fbos[i].bindForWriting();
                bloomLight.use().setUniform("bloomDivider", (float) dividers[i]);
                quadMesh.drawElements();
            }
        }
    }

    public void passTwoBlurBuffers() {
        for (int i = 0; i < fbos.length; i++) {
            blur9x9.use().setUniform("bloomDivider", (float) dividers[i]);

            float scaleH = 1.0f / (colorTextures[i].getWidth());
            float scaleV = 1.0f / (tempColorTextures[i].getHeight());

            int jmax = log(dividers[i], 2);
            for (int j = 0; j < jmax; j++) {
                // Blur horizontally
                colorTextures[i].bind();
                tempFbos[i].bindForWriting();
                blur9x9.use().setUniform("blurScale", scaleH, 0);
                quadMesh.drawElements();

                // Blur vertically
                tempColorTextures[i].bind();
                fbos[i].bindForWriting();
                blur9x9.use().setUniform("blurScale", 0, scaleV);
                quadMesh.drawElements();
            }
        }
    }

    public void passThreeCombine() {
        this.bindTextures();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        bloomCombine.use();
        quadMesh.drawElements();
        glDisable(GL_BLEND);

    }

    static int log(int x, int base) {
        return (int) (Math.log(x) / Math.log(base));
    }

    private void bindTextures() {
        for (int i = 0; i < fbos.length; i++) {
            Texture2D.activeSampler(i);
            colorTextures[i].bind();
        }
    }

    public void drawDebugBuffers(int startx, int endx, int y) {
        int step = (endx - startx) / fbos.length;
        int stepy = y / fbos.length;
        for (int i = 0; i < fbos.length; i++) {
            fbos[i].bindForReading();
            GL30.glBlitFramebuffer(0, 0, colorTextures[i].getWidth(), colorTextures[i].getHeight(),
                    startx + (step * i), stepy * (fbos.length - 1), startx + (step * (i + 1)), stepy * fbos.length, GL_COLOR_BUFFER_BIT, GL_LINEAR);
        }
    }
}
