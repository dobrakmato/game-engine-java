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
package eu.matejkormuth.lpsim.content.bf;

import eu.matejkormuth.lpsim.Geometry;
import eu.matejkormuth.lpsim.content.Content;
import eu.matejkormuth.lpsim.content.StreamUtils;
import eu.matejkormuth.math.vectors.Vector2f;
import eu.matejkormuth.math.vectors.Vector3f;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Arrays;

/**
 * Binary geometry format.
 */
@Slf4j
@UtilityClass
public class BGF {

    public static final byte[] MAGIC = BFConstants.MAGIC_GEOMETRY;
    public static final byte VERSION = 1;

    private static final byte COMPRESSION_NONE = 0;
    private static final byte COMPRESSION_ZIP = 1;

    // Sections:
    private static final byte LIST_POSITIONS = 0;
    private static final byte LIST_NORMALS = 1;
    private static final byte LIST_TEXCOORDS = 2;
    private static final byte LIST_TANGENTS = 3;
    private static final byte LIST_BITANGENTS = 4;
    private static final byte LIST_INDICES = 5;

    // List types:
    private static final byte TYPE_UBYTE = 0; // max 255
    private static final byte TYPE_USHORT = 1; // max 65535
    private static final byte TYPE_UINT = 2; // max 4294967295
    private static final byte TYPE_ULONG = 3; // max 1.8446744e+19

    private static final byte END_OF_LISTS = 127;

    public static Geometry load(String file) {
        file = file.endsWith(".bgf") ? file : file + ".bgf"; // Fix file name.
        try {
            log.info("Loading file {}...", file);
            return Importer.import_(Content.getContent().openRead("models", file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void save(String file, Geometry geometry) {
        file = file.endsWith(".bgf") ? file : file + ".bgf"; // Fix file name.
        try {
            Exporter.export(geometry, Content.getContent().openWrite("models", file));
            log.info("Exported file {}!", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Importer.
     */
    private static final class Importer {
        private static Geometry import_(InputStream stream) throws IOException {
            Geometry geometry = new Geometry();
            DataInputStream in = new DataInputStream(stream);

            byte[] actualMagic = new byte[3];
            in.readFully(actualMagic);

            if (!Arrays.equals(actualMagic, MAGIC)) {
                throw new RuntimeException("Invalid file! Trying to open " + BFConstants.getTypeName(actualMagic) + " as Geometry!");
            }

            byte actualVersion = in.readByte();

            if (actualVersion > VERSION) {
                throw new RuntimeException("Reading file with greater version " + actualVersion + " then supported " + VERSION + "!");
            }

            boolean endOfLists = false;
            while (!endOfLists && in.available() > 0) {
                switch (in.readByte()) {
                    case LIST_NORMALS:
                        readNormals(geometry, in);
                        break;
                    case LIST_POSITIONS:
                        readPositions(geometry, in);
                        break;
                    case LIST_BITANGENTS:
                        readBitangents(geometry, in);
                        break;
                    case LIST_TANGENTS:
                        readTangents(geometry, in);
                        break;
                    case LIST_TEXCOORDS:
                        readTexCoords(geometry, in);
                        break;
                    case LIST_INDICES:
                        readIndices(geometry, in);
                        break;
                    case END_OF_LISTS:
                        endOfLists = true;
                        break;
                    default:
                        throw new RuntimeException("Invalid file! Invalid section ID!");
                }
            }

            in.close();

            return geometry;
        }

        private static void readNormals(Geometry geometry, DataInputStream in) throws IOException {
            int length = in.readInt();
            Vector3f[] normals = new Vector3f[length];

            for (int i = 0; i < length; i++) {
                normals[i] = StreamUtils.readVector3f(in);
            }

            geometry.setNormals(normals);
        }

        private static void readPositions(Geometry geometry, DataInputStream in) throws IOException {
            int length = in.readInt();
            Vector3f[] positions = new Vector3f[length];

            for (int i = 0; i < length; i++) {
                positions[i] = StreamUtils.readVector3f(in);
            }

            geometry.setPositions(positions);
        }

        private static void readBitangents(Geometry geometry, DataInputStream in) throws IOException {
            int length = in.readInt();
            Vector3f[] bitangents = new Vector3f[length];

            for (int i = 0; i < length; i++) {
                bitangents[i] = StreamUtils.readVector3f(in);
            }

            geometry.setBitangents(bitangents);
        }

        private static void readTangents(Geometry geometry, DataInputStream in) throws IOException {
            int length = in.readInt();
            Vector3f[] tangets = new Vector3f[length];

            for (int i = 0; i < length; i++) {
                tangets[i] = StreamUtils.readVector3f(in);
            }

            geometry.setTangents(tangets);
        }

        private static void readTexCoords(Geometry geometry, DataInputStream in) throws IOException {
            int length = in.readInt();
            Vector2f[] texCoords = new Vector2f[length];

            for (int i = 0; i < length; i++) {
                texCoords[i] = StreamUtils.readVector2f(in);
            }

            geometry.setTexCoords(texCoords);
        }

        private static void readIndices(Geometry geometry, DataInputStream in) throws IOException {
            int length = in.readInt();
            int[] indices = new int[length];

            for (int i = 0; i < length; i++) {
                indices[i] = in.readInt();
            }

            geometry.setIndices(indices);
        }
    }

    /**
     * Exporter.
     */
    private static final class Exporter {

        private static void export(Geometry geometry, OutputStream stream) throws IOException {
            DataOutputStream out = new DataOutputStream(stream);
            out.write(MAGIC);
            out.write(VERSION);

            writePositions(geometry, out);
            writeNormals(geometry, out);
            writeTangents(geometry, out);
            writeBitangents(geometry, out);
            writeTexCoords(geometry, out);
            writeIndices(geometry, out);

            out.write(END_OF_LISTS);

            out.close();
        }

        private static void writeIndices(Geometry geometry, DataOutputStream out) throws IOException {
            int[] indices = geometry.getIndices();

            out.write(LIST_INDICES);
            out.writeInt(indices.length);

            for (int i = 0; i < indices.length; i++) {
                out.writeInt(indices[i]);
            }
        }

        private static void writeTexCoords(Geometry geometry, DataOutputStream out) throws IOException {
            Vector2f[] texCoords = geometry.getTexCoords();

            if (texCoords == null) {
                log.warn("Geometry {} has no texCoords! Nothing to write!", geometry);
                return;
            }

            out.write(LIST_TEXCOORDS);
            out.writeInt(texCoords.length);

            Vector2f current = null;

            for (int i = 0; i < texCoords.length; i++) {
                current = texCoords[i];

                out.writeFloat(current.getX());
                out.writeFloat(current.getY());
            }
        }

        private static void writePositions(Geometry geometry, DataOutputStream out) throws IOException {
            Vector3f[] positions = geometry.getPositions();

            if (positions == null) {
                log.warn("Geometry {} has no positions! Nothing to write!", geometry);
                return;
            }

            out.write(LIST_POSITIONS);
            out.writeInt(positions.length);

            Vector3f current = null;

            for (int i = 0; i < positions.length; i++) {
                current = positions[i];

                StreamUtils.write(out, current);
            }
        }

        private static void writeNormals(Geometry geometry, DataOutputStream out) throws IOException {
            Vector3f[] normals = geometry.getNormals();

            if (normals == null) {
                log.warn("Geometry {} has no normals! Nothing to write!", geometry);
                return;
            }

            out.write(LIST_NORMALS);
            out.writeInt(normals.length);

            Vector3f current = null;

            for (int i = 0; i < normals.length; i++) {
                current = normals[i];
                StreamUtils.write(out, current);
            }
        }

        private static void writeTangents(Geometry geometry, DataOutputStream out) throws IOException {
            Vector3f[] tangents = geometry.getTangents();

            if (tangents == null) {
                log.warn("Geometry {} has no tangents! Nothing to write!", geometry);
                return;
            }

            out.write(LIST_TANGENTS);
            out.writeInt(tangents.length);

            Vector3f current = null;

            for (int i = 0; i < tangents.length; i++) {
                current = tangents[i];
                StreamUtils.write(out, current);
            }
        }

        private static void writeBitangents(Geometry geometry, DataOutputStream out) throws IOException {
            Vector3f[] bitangents = geometry.getBitangents();

            if (bitangents == null) {
                log.warn("Geometry {} has no bitangents! Nothing to write!", geometry);
                return;
            }

            out.write(LIST_BITANGENTS);
            out.writeInt(bitangents.length);

            Vector3f current = null;

            for (int i = 0; i < bitangents.length; i++) {
                current = bitangents[i];
                StreamUtils.write(out, current);
            }
        }
    }
}
