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

import lombok.Data;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_COMPRESSED_RGB;
import static org.lwjgl.opengl.GL13.GL_COMPRESSED_RGBA;
import static org.lwjgl.opengl.GL30.GL_COMPRESSED_RED;
import static org.lwjgl.opengl.GL30.GL_COMPRESSED_RG;
import static org.lwjgl.opengl.GL30.GL_RG;

@Data
public final class Format {

    /**
     * Number of channels.
     */
    private final int channels; // 1, 2, 3, 4

    /**
     * Bits per pixel for each channel.
     */
    private final int bitsPerPixel; // 8, 16, 24, 32

    /**
     * Color space of this format. Either LINEAR or GAMMA.
     */
    private final ColorSpace colorSpace; // LINEAR, GAMMA

    public Format(int channels, int bitsPerPixel, ColorSpace colorSpace) {
        this.channels = channels;
        this.bitsPerPixel = bitsPerPixel;
        this.colorSpace = colorSpace;

        if (bitsPerPixel != 8) {
            throw new UnsupportedOperationException("Not yet implemented!");
        }
    }

    public static final Format G_R8G8B8 = new Format(3, 8, ColorSpace.LINEAR);
    public static final Format L_R8G8B8 = new Format(3, 8, ColorSpace.GAMMA);

    public static final Format G_R8 = new Format(1, 8, ColorSpace.LINEAR);
    public static final Format L_R8 = new Format(1, 8, ColorSpace.GAMMA);

    public static int getGlInternalFormat(Format format) {
        switch (format.getColorSpace()) {
            case GAMMA:
                switch (format.getChannels()) {
                    case 1:
                        throw new UnsupportedOperationException("gamma 1 channel");
                    case 2:
                        throw new UnsupportedOperationException("gamma 2 channels");
                    case 3:
                        return GL21.GL_SRGB8;
                    case 4:
                        return GL21.GL_SRGB8_ALPHA8;
                    default:
                        throw new RuntimeException("Invalid num of channels");
                }
            case LINEAR:
                switch (format.getChannels()) {
                    case 1:
                        return GL30.GL_R8;
                    case 2:
                        return GL30.GL_RG8;
                    case 3:
                        return GL11.GL_RGB8;
                    case 4:
                        return GL11.GL_RGBA8;
                    default:
                        throw new RuntimeException("Invalid num of channels");
                }
            default:
                throw new RuntimeException("Invalid color space.");
        }
    }

    public static int getGlFormat(Format format) {
        switch (format.getChannels()) {
            case 1:
                return GL_RED;
            case 2:
                return GL_RG;
            case 3:
                return GL11.GL_RGB;
            case 4:
                return GL11.GL_RGBA;
            default:
                throw new RuntimeException("Invalid num of channels");
        }
    }

    public static int getGlCompressedInternalFormat(Format format) {
        switch (format.getColorSpace()) {
            case GAMMA:
                switch (format.getChannels()) {
                    case 1:
                        throw new UnsupportedOperationException("gamma 1 channel");
                    case 2:
                        throw new UnsupportedOperationException("gamma 2 channels");
                    case 3:
                        return GL21.GL_COMPRESSED_SRGB;
                    case 4:
                        return GL21.GL_COMPRESSED_SRGB_ALPHA;
                    default:
                        throw new RuntimeException("Invalid num of channels");
                }
            case LINEAR:
                switch (format.getChannels()) {
                    case 1:
                        return GL_COMPRESSED_RED;
                    case 2:
                        return GL_COMPRESSED_RG;
                    case 3:
                        return GL_COMPRESSED_RGB;
                    case 4:
                        return GL_COMPRESSED_RGBA;
                    default:
                        throw new RuntimeException("Invalid num of channels");
                }
            default:
                throw new RuntimeException("Invalid color space.");
        }
    }
}
