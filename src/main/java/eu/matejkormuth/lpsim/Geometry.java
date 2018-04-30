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


import eu.matejkormuth.math.vectors.Vector2f;
import eu.matejkormuth.math.vectors.Vector3f;
import lombok.Getter;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;

public class Geometry {

    public static final Geometry QUAD = quad();
    public static final Geometry CUBE = cube();

    private static final Logger log = LoggerFactory.getLogger(Geometry.class);

    /**
     * Whether we should by default compute normals for geometries without normals.
     */
    public static final boolean DEFAULT_COMPUTE_NORMALS = true;

    /**
     * Vertex Positions (vec3)
     */
    @Getter
    private Vector3f[] positions;

    /**
     * Vertex Normals (vec3)
     */
    @Getter
    private Vector3f[] normals;

    /**
     * Vertex Tex-Coords (UV/ST mapping)
     */
    @Getter
    private Vector2f[] texCoords;

    /**
     * Faces (as indices list)
     */
    @Getter
    private int[] indices;

    /**
     * Vertex Tangents (vec3)
     */
    @Getter
    private Vector3f[] tangents;

    /**
     * Vertex Bitangens (vec3)
     */
    @Getter
    private Vector3f[] bitangents;

    public boolean hasNormals() {
        return normals != null && normals.length > 0;
    }

    public boolean hasTangents() {
        return tangents != null && tangents.length > 0;
    }

    public boolean hasBitangets() {
        return bitangents != null && bitangents.length > 0;
    }

    public boolean hasTexCoords() {
        return texCoords != null && texCoords.length > 0;
    }

    public boolean hasFaces() {
        return indices != null && indices.length > 2;
    }

    public Geometry computeNormals() {
        long start = System.nanoTime();
        if (!hasFaces()) {
            throw new IllegalStateException("Can't compute normals for geometry that has no faces!");
        }

        normals = new Vector3f[positions.length];

        if ((indices.length % 3) != 0) {
            throw new RuntimeException("We have incomplete face.");
        }

        log.info("Computing normals for {}. Positions: {}, Indices: {}, Faces: {}", this, positions.length, indices.length, indices.length / 3);

        for (int i = 0; i < normals.length; i++) {
            normals[i] = new Vector3f();
        }

        for (int l = 0; l < indices.length; l += 3) {
            int i = indices[l];
            int j = indices[l + 1];
            int k = indices[l + 2];

            Vector3f n = computeSurfaceNormal(i, j, k);
            normals[i] = normals[i].add(n);
            normals[j] = normals[j].add(n);
            normals[k] = normals[k].add(n);
        }

        for (int i = 0; i < normals.length; i++) {
            normals[i] = normals[i].normalize();
        }

        log.info("{} normals computed in {} ms.", normals.length, (System.nanoTime() - start) / 1_000_000f);
        return this;
    }

    private Vector3f computeSurfaceNormal(int i, int j, int k) {
        Vector3f a = positions[j].subtract(positions[i]);
        Vector3f b = positions[k].subtract(positions[i]);
        return a.cross(b).normalize();
    }

    public Geometry computeTangents() {
        return computeTangents(false);
    }

    public Geometry computeTangents(boolean computeBitangents) {
        long start = System.nanoTime();
        if (!hasFaces()) {
            throw new IllegalStateException("Can't compute tangents for geometry that has no faces!");
        }

        if (!hasTexCoords()) {
            throw new IllegalStateException("Can't compute tangents for geometry that has no texCoords!");
        }

        if ((indices.length % 3) != 0) {
            throw new RuntimeException("We have incomplete face.");
        }

        log.info("Computing tangents for {}. Positions: {}, Indices: {}, Faces: {}", this, positions.length, indices.length, indices.length / 3);

        tangents = new Vector3f[positions.length];
        if (computeBitangents) {
            bitangents = new Vector3f[positions.length];
        }

        for (int i = 0; i < tangents.length; i++) {
            tangents[i] = new Vector3f();
            if (computeBitangents) {
                bitangents[i] = new Vector3f();
            }
        }

        for (int i = 0; i < indices.length; i += 3) {
            int i0 = indices[i];
            int i1 = indices[i + 1];
            int i2 = indices[i + 2];

            Vector3f edge1 = positions[i1].subtract(positions[i0]);
            Vector3f edge2 = positions[i2].subtract(positions[i0]);

            Vector2f v0tex = texCoords[i0];
            Vector2f v1tex = texCoords[i1];
            Vector2f v2tex = texCoords[i2];

            float deltaU1 = v1tex.getX() - v0tex.getX();
            float deltaV1 = v1tex.getY() - v0tex.getY();
            float deltaU2 = v2tex.getX() - v0tex.getX();
            float deltaV2 = v2tex.getY() - v0tex.getY();

            float f = 1.0f / (deltaU1 * deltaV2 - deltaU2 * deltaV1);

            Vector3f tangent = new Vector3f(
                    f * (deltaV2 * edge1.getX() - deltaV1 * edge2.getX()),
                    f * (deltaV2 * edge1.getY() - deltaV1 * edge2.getY()),
                    f * (deltaV2 * edge1.getZ() - deltaV1 * edge2.getZ())
            );

            tangents[i0] = tangents[i0].add(tangent);
            tangents[i1] = tangents[i1].add(tangent);
            tangents[i2] = tangents[i2].add(tangent);
        }

        for (int i = 0; i < tangents.length; i++) {
            tangents[i] = tangents[i].normalize();
            if (computeBitangents) {
                bitangents[i] = tangents[i].cross(normals[i]).normalize();
            }
        }

        log.info("{} tangents computed in {} ms.", normals.length, (System.nanoTime() - start) / 1_000_000f);
        return this;
    }


    public IntBuffer createIndicesBuffer() {
        return (IntBuffer) BufferUtils
                .createIntBuffer(indices.length)
                .put(indices)
                .flip();
    }

    public VertexBufferBuilder.AttributeDataSource getPositionsDataSource() {
        return (index, builder) -> new float[]{
                positions[index].getX(),
                positions[index].getY(),
                positions[index].getZ()
        };
    }

    public VertexBufferBuilder.AttributeDataSource getNormalsDataSource() {
        if (!hasNormals()) {
            log.error("This geometry {} has no normals!", this);
        }

        return (index, builder) -> new float[]{
                normals[index].getX(),
                normals[index].getY(),
                normals[index].getZ()
        };
    }

    public VertexBufferBuilder.AttributeDataSource getTexCoordsDataSource() {
        if (!hasTexCoords()) {
            log.error("This geometry {} has no texCoords!", this);
        }

        return (position, builder) -> new float[]{
                texCoords[position].getX(),
                texCoords[position].getY()
        };
    }

    public VertexBufferBuilder.AttributeDataSource getTangentsDataSource() {
        if (!hasTangents()) {
            log.error("This geometry {} has no tangets!", this);
        }

        return (position, builder) -> new float[]{
                tangents[position].getX(),
                tangents[position].getY(),
                tangents[position].getZ()
        };
    }

    public VertexBufferBuilder.AttributeDataSource getBitangentsDataSource() {
        if (!hasBitangets()) {
            log.error("This geometry {} has no tangets!", this);
        }

        return (position, builder) -> new float[]{
                bitangents[position].getX(),
                bitangents[position].getY(),
                bitangents[position].getZ()
        };
    }

    public static Geometry plane(int size) {
        return plane(size, size);
    }

    public static Geometry plane(int sizeX, int sizeZ) {
        Geometry g = new Geometry();
        g.positions = new Vector3f[sizeX * sizeZ];
        g.normals = new Vector3f[sizeX * sizeZ];
        g.indices = new int[6 * (sizeX - 1) * (sizeZ - 1)];

        // Generate positions and normals.
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                int offset = sizeX * x + z;

                g.positions[offset] = new Vector3f(x, 0, z);
                g.normals[offset] = new Vector3f(0, 1, 0);
            }
        }

        // Build triangles.
        int i = 0;
        for (int x = 0; x < sizeX - 1; x++) {
            for (int z = 0; z < sizeZ - 1; z++) {
                int n = x * sizeX + z;
                // First triangle.
                g.indices[i++] = n;
                g.indices[i++] = n + 1;
                g.indices[i++] = n + sizeZ;

                // Second triangle.
                g.indices[i++] = n + 1;
                g.indices[i++] = n + sizeZ + 1;
                g.indices[i++] = n + sizeZ;
            }
        }

        return g;
    }

    public void setPositions(Vector3f[] positions) {
        this.positions = positions;
    }

    public void setNormals(Vector3f[] normals) {
        this.normals = normals;
    }

    public void setTexCoords(Vector2f[] texCoords) {
        this.texCoords = texCoords;
    }

    public void setIndices(int[] indices) {
        this.indices = indices;
    }

    public void setTangents(Vector3f[] tangents) {
        this.tangents = tangents;
    }

    public void setBitangents(Vector3f[] bitangents) {
        this.bitangents = bitangents;
    }

    public static Geometry quad() {
        Geometry g = new Geometry();
        g.setPositions(new Vector3f[]{
                new Vector3f(-1, -1, 0),
                new Vector3f(1, -1, 0),
                new Vector3f(1, 1, 0),
                new Vector3f(-1, 1, 0)
        });
        g.setTexCoords(new Vector2f[]{
                new Vector2f(0, 0),
                new Vector2f(1, 0),
                new Vector2f(1, 1),
                new Vector2f(0, 1)
        });
        g.setIndices(new int[]{3, 1, 0, 3, 2, 1});
        return g;
    }

    public static Geometry cube() {
        Geometry g = new Geometry();
        g.setPositions(new Vector3f[]{
                new Vector3f(1.000000f, -1.000000f, -1.000000f),
                new Vector3f(1.000000f, -1.000000f, 1.000000f),
                new Vector3f(-1.000000f, -1.000000f, 1.000000f),
                new Vector3f(-1.000000f, -1.000000f, -1.000000f),
                new Vector3f(1.000000f, 1.000000f, -0.999999f),
                new Vector3f(0.999999f, 1.000000f, 1.000001f),
                new Vector3f(-1.000000f, 1.000000f, 1.000000f),
                new Vector3f(-1.000000f, 1.000000f, -1.000000f)
        });
        g.setIndices(new int[]{
                1, 0, 0, 2, 1, 0, 3, 2, 0, 7, 0, 1, 6, 1, 1, 5, 2,
                1, 4, 0, 2, 5, 1, 2, 1, 2, 2, 5, 0, 3, 6, 1, 3, 2,
                2, 3, 2, 3, 4, 6, 0, 4, 7, 1, 4, 0, 0, 5, 3, 1, 5,
                7, 2, 5, 0, 3, 0, 1, 0, 0, 3, 2, 0, 4, 3, 1, 7, 0,
                1, 5, 2, 1, 0, 3, 2, 4, 0, 2, 1, 2, 2, 1, 3, 3, 5,
                0, 3, 2, 2, 3, 3, 2, 4, 2, 3, 4, 7, 1, 4, 4, 3, 5,
                0, 0, 5, 7, 2, 5
        });
        return g;
    }
}
