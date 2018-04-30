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

import eu.matejkormuth.lpsim.content.Content;
import eu.matejkormuth.lpsim.gl.Texture2D;
import eu.matejkormuth.lpsim.timing.Profiler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_MAX_TEXTURE_IMAGE_UNITS;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB;
import static org.lwjgl.opengl.GL30.GL_MAX_COLOR_ATTACHMENTS;
import static org.lwjgl.opengl.GL43.GL_MAX_LABEL_LENGTH;

@Slf4j
public class Application {

    public static final int MSAA_SAMPLES = 0;
    @Getter
    private float anisoMax = 0.0f;

    private static Application singleton;
    @Getter
    private final long timeStart;
    @Getter
    private long frames;

    private Input input;
    private World world;

    private int viewportWidth;
    private int viewportHeight;

    private Queue<Runnable> glThreadTasks = new ConcurrentLinkedQueue<>();

    public static void runFromGLThread(Runnable task) {
        Application.get().glThreadTasks.add(task);
    }

    public Application() {
        if (singleton != null) {
            throw new RuntimeException("Only one instance of application may be running!");
        }
        singleton = this;
        timeStart = System.nanoTime();
    }

    public static class P {
        public static Profiler root = Profiler.createRoot();

        public static Profiler load = root.createChild("Load Level");


        public static Profiler skyboxes = load.createChild("Skyboxes");

        public static Profiler models = load.createChild("Models");
        public static Profiler modelsIO = models.createChild("Disk IO");
        public static Profiler modelsProcessing = models.createChild("Processing");
        public static Profiler modelsParse = modelsProcessing.createChild("Parse");
        public static Profiler modelsDecompress = modelsProcessing.createChild("Decompress");

        public static Profiler textures = load.createChild("Textures");
        public static Profiler texturesIO = textures.createChild("Disk IO");
        public static Profiler texturesProcessing = textures.createChild("Processing");
        public static Profiler texturesParse = texturesProcessing.createChild("Parse");
        public static Profiler texturesDecompress = texturesProcessing.createChild("Decompress");
        public static Profiler texturesDirectBuffer = texturesProcessing.createChild("Direct ByteBuffer");


        public static Profiler texturesSetup = textures.createChild("GPU Texture Setup");
        public static Profiler texturesUpload = textures.createChild("GPU Data Upload");
    }

    public static Input getInput() {
        return singleton.input;
    }

    public static Application get() {
        return singleton;
    }

    public void viewport(int width, int height) {
        if (width != viewportWidth || height != viewportHeight) {
            glViewport(0, 0, width, height);
            viewportHeight = height;
            viewportWidth = width;
        }
    }

    public void start() {
        try {
            initialize();
            loop();
            cleanUp();
        } catch (LWJGLException e) {
            log.error("Can't continue!", e);
        }
    }

    private void initialize() throws LWJGLException {
        Config.getInstance();

        log.info("Creating window...");

        int width = Integer.valueOf(System.getProperty("width", "1920")); //5760
        int height = Integer.valueOf(System.getProperty("height", "1024"));

        Display.setDisplayMode(new DisplayMode(width, height));
        Display.setTitle("Initializing...");
        Display.create(new PixelFormat(0, 0, 0, MSAA_SAMPLES), new ContextAttribs(4, 5).withProfileCore(true)); //new PixelFormat(0, 8, 0, 2), new ContextAttribs(4,2));

        //Display.setVSyncEnabled(true);

        // Output debug information
        System.out.println("Display Adapter: " + Display.getAdapter());
        System.out.println("Display Version: " + Display.getVersion());

        System.out.println("GL_VERSION: " + glGetString(GL11.GL_VERSION));
        System.out.println("GL_VENDOR: " + glGetString(GL11.GL_VENDOR));
        System.out.println("GL_RENDERER: " + glGetString(GL11.GL_RENDERER));
        System.out.println("GL_SHADING_LANGUAGE_VERSION: " + glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
        System.out.println("GL_MAX_VERTEX_ATTRIBS: " + glGetInteger(GL20.GL_MAX_VERTEX_ATTRIBS));
        System.out.println("GL_MAX_LABEL_LENGTH: " + glGetInteger(GL_MAX_LABEL_LENGTH));
        System.out.println("GL_MAX_TEXTURE_SIZE: " + glGetInteger(GL_MAX_TEXTURE_SIZE));
        System.out.println("GL_MAX_TEXTURE_IMAGE_UNITS: " + glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS));
        System.out.println("GL_MAX_COLOR_ATTACHMENTS: " + glGetInteger(GL_MAX_COLOR_ATTACHMENTS));

        if (Bootstrap.DEBUG) { // causes performance problems
            //GL43.glDebugMessageCallback(new KHRDebugCallback(new GLDebugHandler()));
        }

        //GL11.glClearColor(54 / 255f, 140 / 255f, 255 / 255f, 1); // day
        //GL11.glClearColor(14 / 255f, 24 / 255f, 32 / 255f, 1); // night
        GL11.glClearColor(0, 0, 0, 1);

        Keyboard.create();
        Mouse.create();
        Mouse.setGrabbed(true);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glFrontFace(GL11.GL_CW);

        //glEnable(GL30.GL_FRAMEBUFFER_SRGB);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); // alpha rendering blending

        glBlendFunc(GL_ONE, GL_ONE); // forward rendering blending

        //GL11.glEnable(GL13.GL_MULTISAMPLE);
        GL11.glEnable(GL_FRAMEBUFFER_SRGB);
        GL11.glEnable(GL32.GL_TEXTURE_CUBE_MAP_SEAMLESS);

        //int GL_MAX_PATCH_VERTICES = glGetInteger(GL40.GL_MAX_PATCH_VERTICES);
        //log.info("GL_MAX_PATCH_VERTICES: {}", GL_MAX_PATCH_VERTICES);

        //GL40.glPatchParameteri(GL40.GL_PATCH_VERTICES, 3);

        if (GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic) {
            anisoMax = glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
            log.info("GL_EXT_texture_filter_anisotropic (max {}) present! We will have nice texture fitlering!", anisoMax);
        }

        //GL11.glDepthFunc(GL11.GL_LESS);

        //GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        //GL11.glEnable(GL11.GL_TEXTURE_2D);
        //GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

        //glPolygonMode( GL_FRONT_AND_BACK, GL_LINE );

        //GL11.glEnable(GL32.GL_DEPTH_CLAMP);
        Util.checkGLError();

        // Initialize content.
        Content.getContent();

        input = new Input();
        Display.setTitle("Loading...");
        world = new World();
    }

    private void loop() {
        Thread.currentThread().setName("OpenGL Thread");

        long start = System.nanoTime();
        long totalTime = 0;
        //long frames = 0;
        while (!Display.isCloseRequested() && !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            float deltaTime = (System.nanoTime() - start) / 1_000_000f;
            float avgFrameTime = Math.round(totalTime * 100f / (float) frames) * 0.01f;

            float ram = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000;

            if (frames == 10) {
                float startupTime = (System.nanoTime() - timeStart) / 1_000_000f;
                if (startupTime > 10000) {
                    log.warn("Loaded and rendering (10th frame) after {} ms!", startupTime);
                } else {
                    log.info("Loaded and rendering (10th frame) after {} ms!", startupTime);
                }
                World.P.root.reset(true);
            }

            if ((frames % 300) == 0) {
                //log.info("\n" + World.P.root.toString(true));
            }

            totalTime += deltaTime;
            start = System.nanoTime();
            Display.setTitle((int) deltaTime + " ms; AVG " + (int) avgFrameTime + " ms; MSAA" + MSAA_SAMPLES + "x; " + world.pointLights + " lights; " + (int) ram + " MB RAM; " + world.getCamera().getPosition().toString() + " | " + world.getCamera().getForward().toString());
            World.P.root.start();
            update(deltaTime);
            render();
            World.P.root.end();
            Display.update();
            frames++;
        }
    }

    private void render() {
        // Render sky.
        //GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        // Render world.
        world.render();
    }

    private void update(float deltaTime) {
        input(deltaTime);
        // Update world entities and stuff.
        world.update(deltaTime);

        // region Scheduled GL Tasks (texture reloads)
        long start = System.nanoTime();
        if (!glThreadTasks.isEmpty()) {
            int size = glThreadTasks.size();

            try {
                for (int i = 0; !glThreadTasks.isEmpty(); i++) {
                    Display.setTitle("Reloading " + i + "/" + size + "...");
                    glThreadTasks.remove().run();
                }

            } catch (Exception e) {
                log.error("Exception while processing gl task", e);
            }
        }
        long end = System.nanoTime();
        if (end - start > 250000) {
            log.warn("Spent {} ms processing gl tasks from queue!", (end - start) / 1_000_000);
        }
        // endregion
    }

    boolean s1 = false;
    boolean s2 = false;

    private void input(float deltaTime) {
        input.update();

        if (input.wasPressed(Keyboard.KEY_M)) {
            if (s1) {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            } else {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            }
            s1 = !s1;
        }
        if (input.wasPressed(Keyboard.KEY_C)) {
            if (s2) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            } else {
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
            s2 = !s2;
        }
    }

    private void cleanUp() {
        DirectBufferPool.shutdown();
        ShaderCollection.disposeAll();
        Texture2D.disposeAll();
        MemoryUtil.cleanUpDisposables();
        Content.getContent().disposeAll();
    }

    public World getWorld() {
        return world;
    }

}
