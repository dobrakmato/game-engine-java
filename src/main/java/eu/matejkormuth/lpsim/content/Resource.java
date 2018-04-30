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

import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Wrapper class for a resource that can be loaded.
 *
 * @param <T>
 */
public final class Resource<T> {

    private final Class<? extends T> type;

    @Getter
    private final String path;

    @Getter
    private T resource;

    public Resource(String path, @Nonnull Class<? extends T> type) {
        this.type = type;
        this.path = path;
    }

    public CompletableFuture<Resource<T>> loadAsync() {
        if (isLoaded()) {
            return CompletableFuture.completedFuture(this);
        }

        CompletableFuture<Resource<T>> future = new CompletableFuture<>();

        // todo: add to load queue.

        return future;
    }

    /**
     * Unloads the resource from memory and releases all
     * it's buffers.
     */
    public void unload() {

    }

    public boolean hasPath() {
        return path != null;
    }

    /**
     * Returns whether the resource is currently loaded or not.
     *
     * @return boolean indicating whether the resource is currently loaded or not
     */
    public boolean isLoaded() {
        return resource != null;
    }
}
