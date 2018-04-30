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
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.util.Collection;

public class VertexBufferBuilder {

    private final static Logger log = LoggerFactory.getLogger(VertexBufferBuilder.class);

    private final InterleavedVertexLayout bufferLayout;
    private final TIntObjectHashMap<AttributeDataSource> dataSources = new TIntObjectHashMap<>();

    public VertexBufferBuilder(InterleavedVertexLayout bufferLayout) {
        this.bufferLayout = bufferLayout;
    }

    public VertexBufferBuilder bindDataSource(String attribute, AttributeDataSource source) {
        if (bufferLayout.getAttribute(attribute) != null) {
            log.debug("Binding DataSource for '{}' at position {}: {}", attribute, bufferLayout.getAttribute(attribute).getLocation(), source);
            dataSources.put(bufferLayout.getAttribute(attribute).getLocation(), source);
        } else {
            log.warn("Attribute '{}' not found in layout! DataSource not bound.", attribute);
        }
        return this;
    }

    public VertexBufferBuilder bindDataSource(int position, AttributeDataSource source) {
        dataSources.put(position, source);
        return this;
    }

    public AttributeDataSource getDataSource(int position) {
        return dataSources.get(position);
    }

    public AttributeDataSource getDataSource(String attribute) {
        if (bufferLayout.getAttribute(attribute) != null) {
            return dataSources.get(bufferLayout.getAttribute(attribute).getLocation());
        } else {
            throw new IllegalArgumentException("Attribute '" + attribute + "' not found in layout! DataSource not bound!");
        }
    }

    public FloatBuffer create(int vertices) {
        Collection<InterleavedVertexLayout.VertexAttribute> layoutAttributes = bufferLayout.getAttributes();

        int vertexSize = 0;

        // Compute the rowSize of buffer.
        for (InterleavedVertexLayout.VertexAttribute attr : layoutAttributes) {
            vertexSize += attr.getType().getSize();
        }

        log.debug("Vertex size: {} floats", vertexSize);

        FloatBuffer buff = BufferUtils.createFloatBuffer(vertexSize * vertices);

        // Put data to buffer.
        for (int vertex = 0; vertex < vertices; vertex++) {
            for (int attribute = 0; attribute < layoutAttributes.size(); attribute++) {
                AttributeDataSource source = dataSources.get(attribute);

                if (source == null) {
                    throw new IllegalStateException("No configured DataSource for attribute " + attribute +
                            " (" + bufferLayout.getAttribute(attribute).getName() + ")!");
                }

                float[] array = source.dataAt(vertex, this);

                if (array.length != bufferLayout.getAttribute(attribute).getType().getSize()) {
                    throw new IllegalStateException("AttributeDataSource returned " + array.length +
                            " floats but bufferLayout expects " + bufferLayout.getAttribute(attribute).getType().getSize() +
                            " floats!");
                }

                buff.put(array);
            }
        }

        return (FloatBuffer) buff.flip();
    }

    public static VertexBufferBuilder of(InterleavedVertexLayout layout, Geometry geometry) {
        VertexBufferBuilder bufferBuilder = new VertexBufferBuilder(layout);
        bufferBuilder.bindDataSource("position", geometry.getPositionsDataSource());
        bufferBuilder.bindDataSource("normal", geometry.getNormalsDataSource());
        bufferBuilder.bindDataSource("texCoord", geometry.getTexCoordsDataSource());
        bufferBuilder.bindDataSource("tangent", geometry.getTangentsDataSource());
        bufferBuilder.bindDataSource("bitangent", geometry.getBitangentsDataSource());
        return bufferBuilder;
    }

    @FunctionalInterface
    public interface AttributeDataSource {
        float[] dataAt(int vertexIndex, VertexBufferBuilder builder);
    }
}
