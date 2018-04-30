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
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;

import javax.annotation.Nonnull;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

@UtilityClass
@Slf4j
public class DirectBufferPool {

    private static TIntObjectHashMap<Queue<ByteBuffer>> free = new TIntObjectHashMap<>();

    // todo: automatically clean (release from memory) unused buffers

    public static ByteBuffer acquire(int bytes) {
        if (hasOfSize(bytes)) {
            return getOne(bytes);
        } else {
            if (!free.containsKey(bytes)) {
                free.put(bytes, new ArrayDeque<>(2));
            }
            ByteBuffer buff = createOne(bytes);
            return buff;
        }
    }

    private static ByteBuffer createOne(int size) {
        return BufferUtils.createByteBuffer(size);
    }

    private static ByteBuffer getOne(int size) {
        return free.get(size).remove();
    }

    private static boolean hasOfSize(int size) {
        return free.containsKey(size) && !free.get(size).isEmpty();
    }

    public static void release(@Nonnull ByteBuffer buff) {
        buff.clear();
        int size = buff.capacity();
        if (!free.containsKey(size)) {
            throw new RuntimeException("This bytebuffer was not from this pool!");
        }
        free.get(size).add(buff);
    }

    public static void shutdown() {
        log.info("Disposing all pooled direct buffers.");
        long bytes = 0;
        long buffers = 0;
        for (Queue queue : free.valueCollection()) {
            while (!queue.isEmpty()) {
                Buffer buff = (Buffer) queue.remove();
                buffers++;
                bytes += buff.capacity();
                MemoryUtil.clean(buff);
            }
        }
        log.info("Freed {} direct buffers ({} MB)", buffers, (bytes / 1024 / 1024));
    }
}
