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

import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents 2D raster of image with specified amount of bits
 * per channel.
 */
public class Layer {

    public static final int CHANNEL_R = 0;
    public static final int CHANNEL_G = 1;
    public static final int CHANNEL_B = 2;
    public static final int CHANNEL_A = 3;

    private final Image image; // parent

    @Getter
    private final String name;
    @Getter
    private final Format format;
    @Getter
    private final byte[] raster;


    private final TIntObjectHashMap<Channel> channels;

    protected Layer(Image parent, String name, Format format, byte[] raster) {
        this.image = parent;
        this.name = name;
        this.format = format;
        this.raster = raster;

        this.channels = new TIntObjectHashMap<>(format.getChannels());
    }

    @Nullable
    public Channel getChannel(int index) {
        if (!channels.containsKey(index)) {
            channels.put(index, new Channel(this, index));
        }

        return channels.get(index);
    }

    /**
     * Copies this layer's raster to passed layer. The layers have to
     * have same dimensions (width and height) and the same format.
     *
     * @param to layer to copy raster to
     */
    public void copyTo(@Nonnull Layer to) {
        if (!this.format.equals(to.format)) {
            throw new RuntimeException("Layer 'to' has different format than this layer.");
        }

        if (this.getWidth() != to.getWidth() || this.getHeight() != to.getHeight()) {
            throw new RuntimeException("Layer 'to' has different dimensions than this layer.");
        }

        if (to.raster.length != this.raster.length) {
            throw new RuntimeException("Layer 'to' has different raster length than this layer.");
        }

        for (int i = 0; i < this.raster.length; i++) {
            to.raster[i] = this.raster[i];
        }
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    /**
     * Returns length of this layer in number of pixels.
     *
     * @return number of pixels representing this layer
     */
    public int getPixels() {
        return raster.length / (format.getChannels() * (format.getBitsPerPixel() / Byte.SIZE));
    }

    /**
     * Returns length of this layer in bytes.
     *
     * @return length of this layer in bytes
     */
    public int getLength() {
        return raster.length;
    }

    public int getChannelCount() {
        return this.format.getChannels();
    }

    /**
     * Class representing single channel of layer (channel of red color component). It
     * is used as abstraction over main raster array.
     */
    public static final class Channel {

        private final Layer parent;
        private int index;

        public Channel(@Nonnull Layer parent, int index) {
            this.parent = parent;
            this.index = index;
        }

        // 0, 1 G, 255
        public int get(int x, int y) {
            if (x < 0 || y < 0) {
                throw new RuntimeException("x and y must be positive");
            }

            if (x > parent.getWidth()) {
                throw new RuntimeException("x is too high");
            }

            if (y > parent.getHeight()) {
                throw new RuntimeException("y is too high");
            }

            return get(y * parent.image.getWidth() + x);
        }

        public int get(int pos) {
            int bytesPerPixel = parent.format.getBitsPerPixel() * parent.format.getChannels() / Byte.SIZE; // 8
            int position = pos * bytesPerPixel + index;

            if (position > parent.raster.length) {
                throw new RuntimeException("Position is too high!");
            }

            return parent.raster[position];
        }

        public void set(int x, int y, int value) {
            if (x < 0 || y < 0) {
                throw new RuntimeException("x and y must be positive");
            }

            if (x > parent.getWidth()) {
                throw new RuntimeException("x is too high");
            }

            if (y > parent.getHeight()) {
                throw new RuntimeException("y is too high");
            }

            set(y * parent.image.getWidth() + x, value);
        }

        public void set(int pos, int value) {
            int bytesPerPixel = parent.format.getBitsPerPixel() * parent.format.getChannels() / Byte.SIZE; // 8
            int position = pos * bytesPerPixel + index;

            if (position > parent.raster.length) {
                throw new RuntimeException("Position is too high!");
            }

            parent.raster[position] = (byte) value;
        }

        public void copyTo(@Nonnull Channel other) {
            if (this.parent.format.getBitsPerPixel() != other.parent.format.getBitsPerPixel()) {
                throw new RuntimeException("Bits per pixel mismatch!");
            }

            if (this.parent.getPixels() != other.parent.getPixels()) {
                throw new RuntimeException("Layer length mismatch!");
            }

            int length = other.parent.getPixels();
            for (int i = 0; i < length; i++) {
                other.set(i, this.get(i));
            }
        }

    }

}
