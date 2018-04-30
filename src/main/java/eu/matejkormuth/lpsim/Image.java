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

import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Image {

    /**
     * Width of this image and all its layers.
     */
    @Getter
    private int width;

    /**
     * Height of this image and all its layers.
     */
    @Getter
    private int height;

    /**
     * Layer represents raster inside the image.
     * <p>
     * Basic image has exactly one layer called "0". Skyboxes (cubemaps) have
     * exactly 6 layers with names "posx", "negx", "posy", "negy", "posz", "negz".
     */
    private final Map<String, Layer> layers = new HashMap<>();

    /**
     * Creates a new Image with no layers with specified width and height.
     *
     * @param width  width of new image
     * @param height height of new image
     */
    public Image(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a new Image with one default layer named "0"
     * with specified width and height.
     *
     * @param width  width of new image
     * @param height height of new image
     */
    public Image(int width, int height, @Nonnull Format format) {
        this.width = width;
        this.height = height;
        this.layers.put("0", addLayer("0", format));
    }

    @Nullable
    public Layer getLayer(@Nonnull String name) {
        return layers.get(name);
    }

    /**
     * Returns first (not exactly first, layers are not ordered) layer of
     * this image. Useful on basic images with exactly one layer.
     *
     * @return layer
     */
    @Nonnull
    public Layer getLayer() {
        if (!layers.containsKey("0")) {
            throw new RuntimeException("No default layer in this image!");
        }
        return layers.get("0");
    }

    /**
     * Adds new layer to this image with specified name and format.
     *
     * @param name   name of new layer
     * @param format format of new layer
     * @return newly created layer
     */
    public Layer addLayer(@Nonnull String name, @Nonnull Format format) {
        Layer layer = new Layer(this, name, format,
                new byte[width * height * (format.getBitsPerPixel() / Byte.SIZE) * format.getChannels()]
        );
        layers.put(name, layer);
        return layer;
    }

    @Nonnull
    public Collection<Layer> getLayers() {
        return Collections.unmodifiableCollection(layers.values());
    }
}
