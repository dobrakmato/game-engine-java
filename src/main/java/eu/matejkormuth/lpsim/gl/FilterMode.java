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

/**
 * An enum for the texture filtering modes.
 */
public enum FilterMode {
    LINEAR(0x2601, false), // GL11.GL_LINEAR
    NEAREST(0x2600, false), // GL11.GL_NEAREST
    NEAREST_MIPMAP_NEAREST(0x2700, true), // GL11.GL_NEAREST_MIPMAP_NEAREST
    LINEAR_MIPMAP_NEAREST(0x2701, true), //GL11.GL_LINEAR_MIPMAP_NEAREST
    NEAREST_MIPMAP_LINEAR(0x2702, true), // GL11.GL_NEAREST_MIPMAP_LINEAR
    LINEAR_MIPMAP_LINEAR(0x2703, true); // GL11.GL_LINEAR_MIPMAP_LINEAR

    private final int glConstant;
    private final boolean mipmaps;

    FilterMode(int glConstant, boolean mipmaps) {
        this.glConstant = glConstant;
        this.mipmaps = mipmaps;
    }

    /**
     * Gets the OpenGL constant for this texture filter.
     *
     * @return The OpenGL Constant
     */
    public int getGLConstant() {
        return glConstant;
    }

    /**
     * Returns true if the filtering mode required generation of mipmaps.
     *
     * @return Whether or not mipmaps are required
     */
    public boolean needsMipmaps() {
        return mipmaps;
    }
}