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

import lombok.experimental.UtilityClass;
import net.jpountz.lz4.LZ4Factory;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class BFConstants {
    public static final byte[] MAGIC_GEOMETRY = {(byte) 178, 85, 36};
    public static final byte[] MAGIC_IMAGE = {(byte) 178, 85, 35};
    public static final byte[] MAGIC_HEIGHTMAP = {(byte) 178, 85, 34};

    public static final byte VERSION = 1;

    public static final byte COMPRESSION_NONE = 0;
    /**
     * @deprecated
     */
    public static final byte COMPRESSION_DEFLATE = 1;
    public static final byte COMPRESSION_LZ4 = 2;

    private static final Map<byte[], String> names = new HashMap<>();

    static {
        names.put(MAGIC_GEOMETRY, "Geometry");
        names.put(MAGIC_IMAGE, "Imagery");
        names.put(MAGIC_HEIGHTMAP, "Heightmap");
    }

    public static String getTypeName(byte[] magic) {
        return names.get(magic) == null ? "#NOT_A_BF_FILE" : names.get(magic);
    }

    public LZ4Factory getLZ4Factory() {
        return LZ4Factory.fastestInstance();
    }
}
