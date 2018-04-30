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

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;

/**
 * Contains hacks to perform memory optimizations.
 */
@UtilityClass
public class ByteArrayStreamUtil {
    /**
     * ByteArrayOutputStream.buf (byte array)
     */
    private Field BYTE_ARRAY_OUTPUT_STREAM_BUFFER;

    /**
     * ByteArrayOutputStream.count (int)
     */
    private Field BYTE_ARRAY_OUTPUT_STREAM_COUNT;

    static {
        try {
            BYTE_ARRAY_OUTPUT_STREAM_BUFFER = ByteArrayOutputStream.class.getDeclaredField("buf");
            if (!BYTE_ARRAY_OUTPUT_STREAM_BUFFER.isAccessible()) {
                BYTE_ARRAY_OUTPUT_STREAM_BUFFER.setAccessible(true);
            }

            BYTE_ARRAY_OUTPUT_STREAM_COUNT = ByteArrayOutputStream.class.getDeclaredField("count");
            if (!BYTE_ARRAY_OUTPUT_STREAM_COUNT.isAccessible()) {
                BYTE_ARRAY_OUTPUT_STREAM_COUNT.setAccessible(true);
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Unsupported JRE!");
        }
    }

    /**
     * Returns INTERNAL byte array used as buffer in ByteArrayOutputStream. This is used
     * to prevent calling toByteArray() which creates a copy of this array. Thus this saves
     * some memory from garbage collecting.
     *
     * @param stream stream
     * @return internal buffer byte array
     */
    public byte[] getByteArray(ByteArrayOutputStream stream) {
        try {
            int count = BYTE_ARRAY_OUTPUT_STREAM_COUNT.getInt(stream);
            byte[] array = (byte[]) BYTE_ARRAY_OUTPUT_STREAM_BUFFER.get(stream);
            // Check if data we are returning is valid.
            if (array.length != count) {
                throw new IllegalArgumentException("Array length (" +  array.length + ") differs from written bytes (" + count + "), this " +
                        "method will NOT work! Create ByteArrayOutputStream with the right size.");
            }
            return array;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new ByteArrayOutputStream with buffer of specified size in bytes. This
     * method ensures that ByteArrayOutputStream's internal buffer will never be resized
     * when written specified amount of bytes to it.
     *
     * @param bytes size of internal byte array in bytes
     * @return new ByteArrayOutputStream
     */
    public ByteArrayOutputStream createByteArrayOutputStream(int bytes) {
        return new ByteArrayOutputStream(bytes);
    }
}
