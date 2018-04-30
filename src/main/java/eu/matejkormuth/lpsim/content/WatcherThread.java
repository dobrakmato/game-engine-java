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
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public final class WatcherThread extends Thread {

    // One watcher service per file system.
    private final Map<FileSystem, WatchService> services = new ConcurrentHashMap<>();
    private final WatchEvent.Kind[] WATCH_KINDS = {
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
    };

    @Getter
    private List<Consumer<Path>> onModifiedHandlerList = new ArrayList<>(4);

    // Whether is shutdown requested.
    private boolean shutdownRequested = false;

    public WatcherThread() {
        this.setName("IO Watch Thread");
        this.setDaemon(true);
    }

    public void watch(Path path) {
        // todo: Create a task and add it to command queue.
        // todo: Then execute it from watcher thread, not the calling thread.

        FileSystem fs = path.getFileSystem();
        if (!services.containsKey(fs)) {
            try {
                services.put(fs, fs.newWatchService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        WatchService service = services.get(fs);
        try {
            registerRecursively(service, path);
        } catch (IOException e) {
            //throw new RuntimeException(e);
            log.error("Can't watch {}! {}", path.toString(), e.getMessage());
        }
    }

    private void registerRecursively(WatchService service, Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                dir.register(service, WATCH_KINDS);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void run() {
        log.info("Watching for file changes...");
        for (; ; ) {

            if (shutdownRequested) {
                break;
            }

            for (WatchService watcher : services.values()) {

                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException e) {
                    log.error("WatcherThread interrupted:", e);
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        log.error("Event overflow {} {}!", key, watcher);
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) { // invoke callbacks
                        Path path = (Path) event.context();
                        Path dir = (Path) key.watchable();
                        path = dir.resolve(path).toAbsolutePath();

                        if (!path.getFileName().toString().equals("client.log")) { // cliet.log is the name of log file
                            log.info("{} {} modified!", Files.isDirectory(path) ? "Directory" : "File", path.toAbsolutePath());
                        }

                        propagateEvent(onModifiedHandlerList, path.toAbsolutePath());
                    } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path path = (Path) event.context();
                        Path dir = (Path) key.watchable();
                        path = dir.resolve(path).toAbsolutePath();

                        log.info("{} {} created!", Files.isDirectory(path) ? "Directory" : "File", path.toAbsolutePath());
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        Path path = (Path) event.context();
                        Path dir = (Path) key.watchable();
                        path = dir.resolve(path).toAbsolutePath();

                        log.info("{} {} deleted!", Files.isDirectory(path) ? "Directory" : "File", path.toAbsolutePath());
                    }
                }

                key.reset();
            }
        }

        log.info("WatcherThread shutting down...");
        for (WatchService watcher : services.values()) {
            try {
                watcher.close();
            } catch (IOException e) {
                log.warn("Can't close WatcherService!", e);
            }
        }
    }

    private void propagateEvent(List<Consumer<Path>> handlers, Path file) {
        for (Consumer<Path> handler : handlers) {
            try {
                handler.accept(file);
            } catch (Exception e) {
                log.error("Error while propagating event of file {} to handler {}: {}", file, handler, e);
            }
        }
    }

    public void disposeAll() {
        shutdownRequested = true;
    }
}
