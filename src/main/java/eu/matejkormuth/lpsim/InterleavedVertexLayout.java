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
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

public class InterleavedVertexLayout {

    private static final Logger log = LoggerFactory.getLogger(InterleavedVertexLayout.class);

    /**
     * Position only layout.
     */
    public static final InterleavedVertexLayout POSITION_ONLY = InterleavedVertexLayout.builder().attribute("position", AttributeType.VEC3).build();
    public static final InterleavedVertexLayout POSITION_TEXCOORD_ONLY = InterleavedVertexLayout.builder().attribute("position", AttributeType.VEC3).attribute("texCoord", AttributeType.VEC2).build();
    public static final InterleavedVertexLayout STANDARD = InterleavedVertexLayout.builder()
            .attribute("position", InterleavedVertexLayout.AttributeType.VEC3)
            .attribute("normal", InterleavedVertexLayout.AttributeType.VEC3)
            .attribute("texCoord", InterleavedVertexLayout.AttributeType.VEC2)
            .attribute("tangent", InterleavedVertexLayout.AttributeType.VEC3).build();
            // .attribute("bitangent", InterleavedVertexLayout.AttributeType.VEC3).build();

    private TIntObjectHashMap<VertexAttribute> layout = new TIntObjectHashMap<>();

    public InterleavedVertexLayout setAttribute(int location, String name, AttributeType type) {
        this.setAttribute(location, new VertexAttribute(location, name, type));
        return this;
    }

    public void setAttribute(int location, VertexAttribute attribute) {
        attribute.location = location;
        layout.put(location, attribute);
    }

    // O (1)
    public VertexAttribute getAttribute(int location) {
        return layout.get(location);
    }

    // O (n)
    public VertexAttribute getAttribute(String name) {
        int size = this.size();
        for (int i = 0; i < size; i++) {
            if (layout.get(i).name.equals(name)) {
                return layout.get(i);
            }
        }
        return null;
    }

    private int size() {
        return layout.size();
    }

    public Collection<VertexAttribute> getAttributes() {
        return Collections.unmodifiableCollection(layout.valueCollection());
    }
    /**
     * Applies this vertex layout to currently bound VAO.
     */
    public void applyToBoundVAO() {
        applyToBoundVAO(0);
    }

    public void applyToBoundVAO(int indexOffset) {
        int stride = 0;
        for (VertexAttribute attr : layout.valueCollection()) {
            stride += attr.type.bytes;
        }

        int offset = 0;
        for (int i = 0; i < layout.size(); i++) {
            VertexAttribute attr = layout.get(i);

            GL20.glEnableVertexAttribArray(indexOffset + attr.location);
            GL20.glVertexAttribPointer(indexOffset + attr.location, attr.type.size, attr.type.constant, attr.normalized, stride, offset);
            log.debug("VertexAttrib: location = {}, size (in floats) = {}, const = {}, normalized = {}, stride = {}, offset = {}",
                    indexOffset + attr.location, attr.type.size, attr.type.constant, attr.normalized, stride, offset);
            offset += attr.type.bytes;
        }
    }

    // TODO:
    public boolean validateShaderFile(String contents) {
        return true;
    }

    public static VertexLayoutBuilder builder() {
        return new VertexLayoutBuilder();
    }

    public static class VertexLayoutBuilder {
        private final InterleavedVertexLayout layout = new InterleavedVertexLayout();

        public VertexLayoutBuilder attribute(String name, AttributeType type) {
            int location = layout.size();
            layout.setAttribute(location, new VertexAttribute(location, name, type));
            return this;
        }

        public InterleavedVertexLayout build() {
            return layout;
        }
    }

    public static class VertexAttribute {
        @Getter
        private int location;
        @Getter
        private final String name;
        @Getter
        private final AttributeType type;
        private boolean normalized = false;

        public VertexAttribute(int location, String name, AttributeType type) {
            this.name = name;
            this.type = type;
            this.location = location;
        }
    }

    public enum AttributeType {
        FLOAT(1, Float.BYTES, GL11.GL_FLOAT),
        VEC2(2, Float.BYTES * 2, GL11.GL_FLOAT),
        VEC3(3, Float.BYTES * 3, GL11.GL_FLOAT),
        VEC4(4, Float.BYTES * 4, GL11.GL_FLOAT),

        DOUBLE(1, Double.BYTES, GL11.GL_DOUBLE),
        DVEC2(2, Double.BYTES * 2, GL11.GL_DOUBLE),
        DVEC3(3, Double.BYTES * 3, GL11.GL_DOUBLE),
        DVEC4(4, Double.BYTES * 4, GL11.GL_DOUBLE);

        private final int size;
        private final int bytes;
        private final int constant;

        AttributeType(int size, int bytes, int constant) {
            this.size = size;
            this.bytes = bytes;
            this.constant = constant;
        }

        /**
         * Returns size in bytes.
         *
         * @return bytes of this GLSL type
         */
        public int getBytes() {
            return bytes;
        }

        /**
         * Returns GL type constant.
         *
         * @return GL type constant
         */
        public int getConstant() {
            return constant;
        }

        /**
         * Returns size in amount of single values. Size of VEC3 is 3 (floats).
         *
         * @return size in amount of single values in this value
         */
        public int getSize() {
            return size;
        }
    }
}
