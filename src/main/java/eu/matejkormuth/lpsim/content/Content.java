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

import eu.matejkormuth.bf.compression.BFInputStream;
import eu.matejkormuth.bf.image.ImageFile;
import eu.matejkormuth.lpsim.Application;
import eu.matejkormuth.lpsim.gl.Texture2D;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class Content {

    private static final Charset CHARSET = Charset.forName("UTF-8");
    private final List<Path> roots = new ArrayList<>(4);
    private final WatcherThread watcherThread = new WatcherThread();

    private Content() {
        // Register reload handler.
        watcherThread.getOnModifiedHandlerList().add((file) -> {
            // PNG and JPEG Reload handler.
            if (file.toString().endsWith(".png") || file.toString().endsWith(".jpeg") || file.toString().endsWith(".jpg")) {
                log.info("Source file {} updated!",  file);
                String bifPath = file.toAbsolutePath().toString();
                // trim extension
                bifPath = bifPath.substring(0, bifPath.lastIndexOf('.'));
                Path possibleBifFile = Paths.get(bifPath + ".bif").toAbsolutePath();
                if (Files.exists(possibleBifFile)) {
                    log.info("Compile file {} exists.",  possibleBifFile);

                    // Try to determine format from memory
                    List<Texture2D> textures = Texture2D.find(possibleBifFile.toString());
                    if (textures.size() > 0) {
                        Path finalFile1 = file;
                        new Thread(() -> {
                            try {
                                ProcessBuilder builder = determinePng2BifCmd(finalFile1, possibleBifFile, textures.get(0));
                                Process p = builder.start();
                                log.info("Process start");
                                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                while (p.isAlive()) {
                                    log.info("> " + in.readLine());
                                }
                            } catch (IOException e) {
                                log.error("error", e);
                            }
                        }, "OnModifiedHandler/" + possibleBifFile.getFileName().toString()).start();
                    }

                    // todo: Load image from disk
                }
            }

            // BIF Reload Handler
            if (file.toString().endsWith(".bif")) {
                if (!file.isAbsolute()) {
                    file = resolve(file).toAbsolutePath();
                }

                List<Texture2D> textures = Texture2D.find(file.toString());
                if (textures.size() > 0) {
                    final Path finalFile = file;
                    Application.runFromGLThread(() -> {
                        log.info("Reloading {} textures for file {}...", textures.size(), finalFile);
                        for (Texture2D tex : textures) {
                            try {
                                ImageFile.loadIntoTexture(new BFInputStream(openRead(finalFile)), tex);
                            } catch (Exception e) {
                                log.error("Error occured while reloading {}: {}", finalFile, e);
                            }
                        }
                    });
                }
            }
        });
    }

    private ProcessBuilder determinePng2BifCmd(Path source, Path target, Texture2D texture2D) {
        ProcessBuilder process = new ProcessBuilder();
        List<String> cmds = new ArrayList<>();

        cmds.add("java");
        cmds.add("-cp");
        cmds.add("lpsim-1.0.jar");
        cmds.add("eu.matejkormuth.bf.tools.Png2Bif");
        cmds.add("-fast");  // enable fast compression
        cmds.add("-i");
        cmds.add(source.toString());

        if (texture2D.getInternalFormat() == GL21.GL_SRGB8 || texture2D.getInternalFormat() == GL21.GL_SRGB8_ALPHA8) {
            cmds.add("-gm");
        }

        switch (texture2D.getFormat()) {
            case GL11.GL_RED:
                cmds.add("-r");
                cmds.add("r");
                break;
            case GL30.GL_RG:
                cmds.add("-r");
                cmds.add("r");
                cmds.add("-g");
                cmds.add("g");
                break;
            case GL11.GL_RGB:
                cmds.add("-r");
                cmds.add("r");
                cmds.add("-g");
                cmds.add("g");
                cmds.add("-b");
                cmds.add("b");
                break;
            case GL11.GL_RGBA:
                cmds.add("-r");
                cmds.add("r");
                cmds.add("-g");
                cmds.add("g");
                cmds.add("-b");
                cmds.add("b");
                cmds.add("-a");
                cmds.add("a");
                break;
        }



        process.command(cmds);
        process.directory(Paths.get(".").toFile());
        process.redirectErrorStream(true);

        return process;
    }

    // static instance
    private static Content content;

    /*
     *  Configure default content roots here. More roots
     *  may be configured trough scripts or by configuration
     *  file.
     *
     *  First roots are checked first when resolving a path.
     */
    static {
        content = new Content();
        content.addRoot(Paths.get("C:\\Users\\Matej\\IdeaProjects\\lpsim\\src\\main\\resources/"));
        content.addRoot(Paths.get(".")); // default one
        content.addRoot(Paths.get("D:\\Users\\Matej\\IdeaProjects\\lpsim\\src\\main\\resources/"));
        content.addRoot(Paths.get("D:\\Users\\Matej\\IdeaProjects\\lpsim\\development\\Development Resources\\PBR Materials/")); // pbr materials
        content.addRoot(Paths.get("D:\\Users\\Matej\\IdeaProjects\\lpsim\\development\\Development Resources\\__IN_WORK")); // pbr materials (2)
    }

    public static Content getContent() {
        return content;
    }

    public void disposeAll() {
        //watcherThread.interrupt();
        watcherThread.disposeAll();
    }

    public void addRoot(Path root) {
        try {
            Path absoluteRoot = root.toRealPath();
            roots.add(absoluteRoot);
            log.info("Added content root: {}", absoluteRoot);
            watch(absoluteRoot);
        } catch (IOException e) {
            log.error("Can't add root " + root.toString(), e);
        }
    }

    // Starts watching content root for changes and reloads files them when they happen.
    private void watch(Path root) {
        if (!watcherThread.isAlive()) {
            watcherThread.start();
        }
        watcherThread.watch(root);
    }

    public Path resolve(String first, String... more) {
        return resolve(Paths.get(first, more));
    }

    public Path resolve(Path relative) {
        Path resolved;
        for (Path root : roots) {
            resolved = root.resolve(relative);
            if (Files.exists(resolved)) {
                // log.info("Resolved {} at {}", relative.toString(), resolved.toString());
                return resolved;
            }
        }
        throw new IllegalArgumentException("File \"" + relative + "\" not found in any source root!");
    }

    public boolean exists(String first, String... more) {
        return exists(Paths.get(first, more));
    }

    public boolean exists(Path relative) {
        Path resolved;
        for (Path root : roots) {
            resolved = root.resolve(relative);
            if (Files.exists(resolved)) {
                return true;
            }
        }
        return false;
    }

    public InputStream openRead(String first, String... more) {
        return openRead(Paths.get(first, more));
    }

    public InputStream openRead(Path relative) {
        try {

            FileChannel fch = FileChannel.open(resolve(relative), StandardOpenOption.READ);
            int length = (int) fch.size();
            byte[] buffer = new byte[length];
            ByteBuffer data = ByteBuffer.wrap(buffer);

            int readBytes = 0;
            while (readBytes != length) {
                readBytes += fch.read(data);
            }

            return new ByteArrayInputStream(buffer);
            //return new BufferedInputStream(new FileInputStream(resolve(relative).toFile()));
        } catch (IOException e) {
            // shouldn't happen
            throw new RuntimeException(e);
        }
    }

    public OutputStream openWrite(String first, String... more) {
        return openWrite(Paths.get(first, more));
    }

    public OutputStream openWrite(Path relative) {
        try {
            return new BufferedOutputStream(new FileOutputStream(resolve(relative).toFile()));
        } catch (FileNotFoundException e) {
            // shouldn't happen
            throw new RuntimeException(e);
        }
    }

    public byte[] readFile(String first, String... more) {
        return readFile(Paths.get(first, more));
    }

    public byte[] readFile(Path relative) {
        try {
            return Files.readAllBytes(resolve(relative));
        } catch (IOException e) {
            throw new RuntimeException("Can't read file!", e);
        }
    }

    public String readText(String first, String... more) {
        return readText(Paths.get(first, more));
    }

    public String readText(Path relative) {
        return new String(readFile(relative), CHARSET);
    }

    public List<String> readLines(String first, String... more) {
        return readLines(Paths.get(first, more));
    }

    public List<String> readLines(Path relative) {
        try {
            return Files.readAllLines(resolve(relative), CHARSET);
        } catch (IOException e) {
            throw new RuntimeException("Can't read lines!", e);
        }
    }


}
