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
package eu.matejkormuth.lpsim.content;

import eu.matejkormuth.lpsim.Application;
import eu.matejkormuth.lpsim.MemoryUtil;
import eu.matejkormuth.lpsim.gl.FilterMode;
import eu.matejkormuth.lpsim.gl.Texture2D;
import eu.matejkormuth.lpsim.gl.TextureCube;
import eu.matejkormuth.lpsim.gl.WrapMode;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static javax.imageio.ImageIO.read;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL21.GL_SRGB8_ALPHA8;

@Slf4j
@UtilityClass
public class AWTDecoder {
    public static Texture2D loadGamma(String file) {
        return load(Content.getContent().openRead("textures", file), true);
    }

    public static Texture2D loadLinear(String file) {
        return load(Content.getContent().openRead("textures", file), false);
    }

    private static Texture2D load(InputStream is, boolean gamma) {
        try {
            BufferedImage image = read(is);
            int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());

            log.debug("Image W: {}, H: {}, pixels: {}", image.getWidth(), image.getHeight(), pixels.length);

            ByteBuffer texData = BufferUtils.createByteBuffer(pixels.length * 4);
            boolean hasAlpha = image.getColorModel().hasAlpha();

            int currentPixel;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {

                    //pixel   = rgbArray[offset + (y-startY)*scansize + (x-startX)];
                    currentPixel = pixels[y * image.getWidth() + x];

                    texData.put((byte) ((currentPixel >> 16) & 0xFF));
                    texData.put((byte) ((currentPixel >> 8) & 0xFF));
                    texData.put((byte) ((currentPixel) & 0xFF));
                    if (hasAlpha) {
                        texData.put((byte) ((currentPixel >> 24) & 0xFF));
                    } else {
                        texData.put((byte) 0xFF);
                    }
                }
            }
            texData.flip();

            //Texture2D.activeSampler(0);
            Texture2D texture2D = Texture2D.create();

            texture2D.bind();
            texture2D.setImageData(texData, GL_RGBA, gamma ? GL_SRGB8_ALPHA8 : GL_RGBA8, image.getWidth(), image.getHeight());
            texture2D.generateMipmaps();
            texture2D.setWraps(WrapMode.REPEAT, WrapMode.REPEAT);
            texture2D.setFilters(FilterMode.LINEAR_MIPMAP_LINEAR, FilterMode.LINEAR);

            if (Application.get().getAnisoMax() != 0.0f) {
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, Application.get().getAnisoMax());
            }

            Util.checkGLError();
            return texture2D;

        } catch (IOException e) {
            log.error("Can't decode image!", e);
            throw new RuntimeException(e);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {

            }
        }
    }

    /*
     * PosX = Front
     * PosY = Up
     * PosZ = Right
     * NegX = Back
     * NegY = Down
     * NegZ = Left
     */

    public static TextureCube loadSkybox(String name, String ext) {
        try {
            BufferedImage front = ImageIO.read(Content.getContent().openRead("skyboxes", name, name + "_ft." + ext));
            ByteBuffer posX = toByteBuffer(front);
            ByteBuffer posY = toByteBuffer(ImageIO.read(Content.getContent().openRead("skyboxes", name, name + "_up." + ext)));
            ByteBuffer posZ = toByteBuffer(ImageIO.read(Content.getContent().openRead("skyboxes", name, name + "_rt." + ext)));
            ByteBuffer negX = toByteBuffer(ImageIO.read(Content.getContent().openRead("skyboxes", name, name + "_bk." + ext)));
            ByteBuffer negY = toByteBuffer(ImageIO.read(Content.getContent().openRead("skyboxes", name, name + "_dn." + ext)));
            ByteBuffer negZ = toByteBuffer(ImageIO.read(Content.getContent().openRead("skyboxes", name, name + "_lf." + ext)));

            TextureCube cube = TextureCube.create();
            cube.bind();
            cube.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
            cube.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
            Util.checkGLError();
            cube.setImageData(front.getWidth(), front.getHeight(), GL_RGBA, GL_RGBA, posX, posY, posZ, negX, negY, negZ);

            MemoryUtil.clean(posX);
            MemoryUtil.clean(posY);
            MemoryUtil.clean(posZ);
            MemoryUtil.clean(negX);
            MemoryUtil.clean(negZ);
            MemoryUtil.clean(negZ);

            Util.checkGLError();
            return cube;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ByteBuffer toByteBuffer(BufferedImage image) {
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        ByteBuffer texData = BufferUtils.createByteBuffer(pixels.length * 4);
        boolean hasAlpha = image.getColorModel().hasAlpha();

        int currentPixel;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {

                //pixel   = rgbArray[offset + (y-startY)*scansize + (x-startX)];
                currentPixel = pixels[y * image.getWidth() + x];

                texData.put((byte) ((currentPixel >> 16) & 0xFF));
                texData.put((byte) ((currentPixel >> 8) & 0xFF));
                texData.put((byte) ((currentPixel) & 0xFF));
                if (hasAlpha) {
                    texData.put((byte) ((currentPixel >> 24) & 0xFF));
                } else {
                    texData.put((byte) 0xFF);
                }
            }
        }
        texData.flip();
        return texData;
    }

    public static TextureCube loadSkybox(String name) {
        return loadSkybox(name, "png");
    }
}
