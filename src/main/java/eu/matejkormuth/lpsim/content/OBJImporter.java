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

import eu.matejkormuth.lpsim.Geometry;
import eu.matejkormuth.math.vectors.Vector2f;
import eu.matejkormuth.math.vectors.Vector3f;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@UtilityClass
public class OBJImporter {

    private static class OBJFile {
        public static final String SPACE = "\\s+";
        public static final String COMMENT = "#";
        public static final String TOKEN_POSITION = "v";
        public static final String TOKEN_TEXCOORD = "vt";
        public static final String TOKEN_NORMAL = "vn";
        public static final String TOKEN_FACE = "f";

        @Getter
        private List<Vector3f> positions = new ArrayList<>();
        @Getter
        private List<Vector3f> normals = new ArrayList<>();
        @Getter
        private List<Vector2f> texCoords = new ArrayList<>();
        @Getter
        private List<OBJIndex> indices = new ArrayList<>();

        public void addPosition(Vector3f position) {
            positions.add(position);
        }

        public void addNormal(Vector3f normal) {
            normals.add(normal);
        }

        public void addTexCoord(Vector2f texCoord) {
            texCoords.add(texCoord);
        }

        public void addIndex(OBJIndex index) {
            indices.add(index);
        }

        private void parseLine(String line) {
            TokenStack lt = new TokenStack(line, OBJFile.SPACE); // line tokens
            switch (lt.next()) {
                case OBJFile.TOKEN_POSITION:
                    addPosition(lt.nextVector3f());
                    break;
                case OBJFile.TOKEN_NORMAL:
                    addNormal(lt.nextVector3f());
                    break;
                case OBJFile.TOKEN_TEXCOORD:
                    addTexCoord(lt.nextVector2f());
                    break;
                case OBJFile.TOKEN_FACE:
                    // TODO: Only 3-index (triangle) faces are supported.
                    addIndex(parseIndex(lt.next()));
                    addIndex(parseIndex(lt.next()));
                    addIndex(parseIndex(lt.next()));
                    // TODO: Support for more later / triangulation.
                    break;
                default:
                    log.debug("Invalid token at line '{}'!", line);
                    break;
            }
        }

        private static OBJIndex parseIndex(String faceDeclaration) {
            OBJIndex index = new OBJIndex();
            if (!faceDeclaration.contains(OBJIndex.SEPARATOR)) {
                index.positionIndex = Integer.valueOf(faceDeclaration);
            } else {
                TokenStack it = new TokenStack(faceDeclaration, OBJIndex.SEPARATOR); // index tokens
                index.positionIndex = it.peekInt() > 0 ? it.nextInt() : intThrow(new RuntimeException("Relative indices are nor supported: " + faceDeclaration));
                index.texCoordIndex = it.peekInt() > 0 ? it.nextInt() : intThrow(new RuntimeException("Relative indices are nor supported: " + faceDeclaration));
                index.normalIndex = it.peekInt() > 0 ? it.nextInt() : intThrow(new RuntimeException("Relative indices are nor supported: " + faceDeclaration));
            }
            return index;
        }

        private static int intThrow(RuntimeException e) {
            throw e;
        }
    }

    @EqualsAndHashCode
    private static class OBJIndex {
        public static final String SEPARATOR = "/";
        public static final int INDEX_UNDEFINED = 0;

        int positionIndex = INDEX_UNDEFINED;
        int normalIndex = INDEX_UNDEFINED;
        int texCoordIndex = INDEX_UNDEFINED;
    }

    private static OBJFile parse(List<String> lines) {
        OBJFile file = new OBJFile();
        lines.stream()
                .filter(s -> !s.startsWith(OBJFile.COMMENT))
                .forEach(file::parseLine);
        return file;
    }

    private static Geometry import_(List<String> lines) {
        Geometry g = new Geometry();
        OBJFile file = parse(lines);

        log.info("Parsed OBJFile: Positions: {}; Normals: {}; TexCoords: {}", file.getPositions().size(),
                file.getNormals().size(), file.getTexCoords().size());
        log.info(" OBJIndex: Count: {}; Distinct: {}", file.getIndices().size(), file.getIndices().stream().distinct().count());

        int vertexCount = (int) file.getIndices().stream().distinct().count();

        int[] indices = new int[file.getIndices().size()];
        Vector3f[] positions = new Vector3f[vertexCount];
        Vector3f[] normals = new Vector3f[vertexCount];
        Vector2f[] texCoords = new Vector2f[vertexCount];

        // step 1

        final int[] lastIndex = {0, 0, 0}; // last index is zero based
        TObjectIntHashMap<OBJIndex> newIndex = new TObjectIntHashMap<>(); // obj index -> new index
        file.getIndices().stream().distinct().forEach(objIndex -> {
            if (objIndex.positionIndex != OBJIndex.INDEX_UNDEFINED) {
                positions[lastIndex[0]] = file.getPositions().get(objIndex.positionIndex - 1);  //objIndex.positionIndex is one-based
            }

            if (objIndex.normalIndex != OBJIndex.INDEX_UNDEFINED) {
                normals[lastIndex[0]] = file.getNormals().get(objIndex.normalIndex - 1); //objIndex.normalIndex is one-based
            }

            if (objIndex.texCoordIndex != OBJIndex.INDEX_UNDEFINED) {
                texCoords[lastIndex[0]] = file.getTexCoords().get(objIndex.texCoordIndex - 1); //objIndex.texCoordIndex is one-based
            }

            newIndex.put(objIndex, lastIndex[0]++);
        });

        // step 2
        final int[] indexIndex = {0}; // indexIndex is zero based (is used to access geometry indices array)
        file.getIndices().stream().forEach(objIndex -> indices[indexIndex[0]++] = newIndex.get(objIndex)); // returned index is zero based

        // step 3
        g.setPositions(positions);
        g.setNormals(normals);
        g.setTexCoords(texCoords);
        g.setIndices(indices);

        return g;
    }

    public static Geometry load(String file) {
        file = file.endsWith(".obj") ? file : file + ".obj"; // Fix file name.
        return import_(Content.getContent().readLines("models", file));
    }

    public static Geometry loadInternal(String file) throws IOException {
        file = file.endsWith(".obj") ? file : file + ".obj"; // Fix file name.
        return import_(Files.readAllLines(Paths.get(file)));
    }

    private static class TokenStack {
        public static final int TOKEN_EMPTY = Integer.MAX_VALUE;
        private final String[] tokens;
        private int index = 0;

        public TokenStack(String string, String delimiter) {
            this.tokens = string.split(delimiter);
        }

        String next() {
            return tokens[index++];
        }

        String peek() {
            return tokens[index];
        }

        float nextFloat() {
            return Float.parseFloat(next());
        }

        int nextInt() {
            if (peek().isEmpty()) {
                return 0;
            }

            return Integer.parseInt(next(), 10);
        }

        int peekInt() {
            if (peek().isEmpty()) {
                return TOKEN_EMPTY;
            }

            return Integer.parseInt(peek(), 10);
        }

        Vector3f nextVector3f() {
            return new Vector3f(nextFloat(), nextFloat(), nextFloat());
        }

        Vector2f nextVector2f() {
            return new Vector2f(nextFloat(), nextFloat());
        }
    }
}
