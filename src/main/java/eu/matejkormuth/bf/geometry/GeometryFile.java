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
package eu.matejkormuth.bf.geometry;

import eu.matejkormuth.bf.BFUtils;
import eu.matejkormuth.bf.compression.BFInputStream;
import eu.matejkormuth.bf.compression.BFOutputStream;
import eu.matejkormuth.lpsim.Application;
import eu.matejkormuth.lpsim.Geometry;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@UtilityClass
@Slf4j
public class GeometryFile {

    public static final byte BF_TYPE = 'G';

    /*
     * Version history:
     * 1 - initial version with LZ4 compression
     */
    public static final byte VERSION = 1;

    private static void verifyCanReadVersion(BFInputStream in) throws IOException {
        if (in.readByte() > VERSION) {
            throw new RuntimeException("Can't read this file! (Version mismatch)");
        }
    }

    private static void readHeader(BFInputStream in) throws IOException {
        BFUtils.readHeader(in);

        if (in.readByte() != BF_TYPE) {
            throw new RuntimeException("Not a BF Image file! (File Type mismatch)");
        }

        verifyCanReadVersion(in);
    }

    private static void writeHeader(BFOutputStream out) throws IOException {
        BFUtils.writeHeader(out);
        out.writeByte(BF_TYPE);
        out.writeByte(VERSION);
    }

    private static final byte LIST_POSITIONS = 1;
    private static final byte LIST_NORMALS = 2;
    private static final byte LIST_TEXCOORDS = 3;
    private static final byte LIST_TANGENTS = 4;
    private static final byte LIST_BITANGENTS = 5;
    private static final byte LIST_INDICES = 6;

    public static void save(Geometry geometry, BFOutputStream out) throws IOException {
        writeHeader(out);

        // 1 byte for section type + 1 integer for array length + array items
        int size = (geometry.hasFaces() ? Byte.BYTES + Integer.BYTES + Float.BYTES * 3 * geometry.getPositions().length : 0) +
                (geometry.hasNormals() ? Byte.BYTES + Integer.BYTES + Float.BYTES * 3 * geometry.getNormals().length : 0) +
                (geometry.hasTexCoords() ? Byte.BYTES + Integer.BYTES + Float.BYTES * 2 * geometry.getTexCoords().length : 0) +
                (geometry.hasTangents() ? Byte.BYTES + Integer.BYTES + Float.BYTES * 3 * geometry.getTangents().length : 0) +
                (geometry.hasBitangets() ? Byte.BYTES + Integer.BYTES + Float.BYTES * 3 * geometry.getBitangents().length : 0) +
                (1 + Integer.BYTES + Integer.BYTES * geometry.getIndices().length);


        out.compressStart(size);

        out.writeByte(LIST_POSITIONS);
        out.writeVector3fArray(geometry.getPositions());

        if (geometry.hasNormals()) {
            out.writeByte(LIST_NORMALS);
            out.writeVector3fArray(geometry.getNormals());
        }

        if (geometry.hasTexCoords()) {
            out.writeByte(LIST_TEXCOORDS);
            out.writeVector2fArray(geometry.getTexCoords());
        }

        if (geometry.hasTangents()) {
            out.writeByte(LIST_TANGENTS);
            out.writeVector3fArray(geometry.getTangents());
        }

        if (geometry.hasBitangets()) {
            out.writeByte(LIST_BITANGENTS);
            out.writeVector3fArray(geometry.getBitangents());
        }

        out.writeByte(LIST_INDICES);
        out.writeIntArray(geometry.getIndices());

        out.compressFinish();
        out.flush();
        out.close();
    }

    public static Geometry loadToGeometry(BFInputStream in, Geometry geometry) throws IOException {
        Application.P.modelsIO.end();

        Application.P.modelsProcessing.start();
        Application.P.modelsParse.start();
        readHeader(in);

        Application.P.modelsParse.end();
        Application.P.modelsDecompress.start();
        in.decompress();
        Application.P.modelsDecompress.end();

        Application.P.modelsParse.start();
        for (; ; ) {
            byte listType = in.readByte();

            if (listType == LIST_INDICES)
                break;

            switch (listType) {
                case LIST_POSITIONS:
                    geometry.setPositions(in.readVector3fArray());
                    break;
                case LIST_NORMALS:
                    geometry.setNormals(in.readVector3fArray());
                    break;
                case LIST_TEXCOORDS:
                    geometry.setTexCoords(in.readVector2fArray());
                    break;
                case LIST_TANGENTS:
                    geometry.setTangents(in.readVector3fArray());
                    break;
                case LIST_BITANGENTS:
                    geometry.setBitangents(in.readVector3fArray());
                    break;
            }
        }

        geometry.setIndices(in.readIntArray());

        in.close();
        Application.P.modelsParse.end();

        return geometry;
    }

}
