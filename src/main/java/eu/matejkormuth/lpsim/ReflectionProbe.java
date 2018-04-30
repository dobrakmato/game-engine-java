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

import eu.matejkormuth.lpsim.gl.*;
import eu.matejkormuth.lpsim.math.Matrix4f;
import eu.matejkormuth.math.vectors.Vector3f;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import static eu.matejkormuth.lpsim.Syntax.vec3;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_RGB16F;


public final class ReflectionProbe {

    private static final int[] TEXTURE_TARGETS = new int[]{
            GL_TEXTURE_CUBE_MAP_POSITIVE_X,
            GL_TEXTURE_CUBE_MAP_NEGATIVE_X,
            GL_TEXTURE_CUBE_MAP_POSITIVE_Y,
            GL_TEXTURE_CUBE_MAP_NEGATIVE_Y,
            GL_TEXTURE_CUBE_MAP_POSITIVE_Z,
            GL_TEXTURE_CUBE_MAP_NEGATIVE_Z
    };

    private static final Vector3f[] TEXTURE_TARGETS_FORWARD_VECTORS = new Vector3f[]{
            vec3(1, 0, 0),
            vec3(-1, 0, 0),
            vec3(0, -1, 0),
            vec3(0, 1, 0),
            vec3(0, 0, 1),
            vec3(0, 0, -1)
    };

    private static final Vector3f[] TEXTURE_TARGETS_UP_VECTORS = new Vector3f[]{
            Vector3f.UNIT_Y,
            Vector3f.UNIT_Y,
            Vector3f.UNIT_Z.negate(),
            Vector3f.UNIT_Z.negate(),
            Vector3f.UNIT_Y,
            Vector3f.UNIT_Y,
    };

    private float Z_NEAR = 0.1f;
    private float Z_FAR = 6000f;

    @Getter
    @Setter
    private Vector3f position;

    @Getter
    private final TextureCube texture;
    private final RenderBuffer[] depthTextures;
    private final FrameBuffer[] fbo;
    private Matrix4f projectionMatrix;
    private final Matrix4f[] viewMatrices;

    private boolean isDynamic = true;
    private int refreshRate = 1; // each one frame
    private int size = 128;

    private boolean matrixDirty = true;


    private final PreethamSky sky;

    public ReflectionProbe(PreethamSky sky) {
        this.sky = sky;

        // create hdr texture
        texture = TextureCube.create();
        texture.bind();
        texture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        texture.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
        texture.setTag("CubeMap (" + this.toString());

        // hdr buffers
        texture.createNegativeXImageData(size, size, GL_RGB, GL_RGB16F);
        texture.createNegativeYImageData(size, size, GL_RGB, GL_RGB16F);
        texture.createNegativeZImageData(size, size, GL_RGB, GL_RGB16F);
        texture.createPositiveXImageData(size, size, GL_RGB, GL_RGB16F);
        texture.createPositiveYImageData(size, size, GL_RGB, GL_RGB16F);
        texture.createPositiveZImageData(size, size, GL_RGB, GL_RGB16F);
        texture.generateMipmaps();

        depthTextures = new RenderBuffer[6];
        fbo = new FrameBuffer[6];
        viewMatrices = new Matrix4f[6];

        /// set-up fbo
        for (int face = 0; face < 6; face++) {
            viewMatrices[face] = new Matrix4f().initIdentity();

            fbo[face] = new FrameBuffer();
            fbo[face].bind();
            fbo[face].setTag("ReflectionProbe (face " + face + ")");

            depthTextures[face] = new RenderBuffer();
            depthTextures[face].bind();
            depthTextures[face].createStorage(GL11.GL_DEPTH_COMPONENT, size, size);

            fbo[face].attach(FrameBufferTarget.FRAMEBUFFER, GL_COLOR_ATTACHMENT0, texture.getId(), TEXTURE_TARGETS[face]);
            fbo[face].attachRenderBuffer(FrameBufferTarget.FRAMEBUFFER, depthTextures[face], GL30.GL_DEPTH_ATTACHMENT);
            fbo[face].checkFramebuffer(FrameBufferTarget.FRAMEBUFFER);
        }
    }

    private void recomputeMatrices() {
        Matrix4f translation = new Matrix4f().initTranslation(-position.getX(), -position.getY(), -position.getZ());
        projectionMatrix = new Matrix4f().initPerspective(90, size, size, Z_NEAR, Z_FAR);

        for (int face = 0; face < 6; face++) {
            Matrix4f rotation = new Matrix4f().initCamera(TEXTURE_TARGETS_FORWARD_VECTORS[face].normalize(), TEXTURE_TARGETS_UP_VECTORS[face]);
            viewMatrices[face] = rotation.multiply(translation);
        }
        matrixDirty = false;
    }

    public void capture() {
        // build matrices
        if (matrixDirty) {
            recomputeMatrices();
        }

        Application.get().viewport(size, size);

        //CameraContext camera = new CameraContext();
        //camera.setPosition(position);
        //camera.setProjection(projectionMatrix);

        Matrix4f translation = new Matrix4f().initTranslation(position.getX(), position.getY(), position.getZ());
        Matrix4f scale = new Matrix4f().initScale(Z_FAR * .99f, Z_FAR * .99f, Z_FAR * .99f);
        Matrix4f model = translation.multiply(scale);

        for (int face = 0; face < 6; face++) {
            //camera.setView(viewMatrices[face]);

            Matrix4f wvp = projectionMatrix.multiply(viewMatrices[face].multiply(model));

            fbo[face].bindForWriting();
            // render skybox
            glCullFace(GL_FRONT);
            glDisable(GL_BLEND);
            glDisable(GL_CULL_FACE);
            glDisable(GL_DEPTH_TEST);

            sky.render(wvp, false);

            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
        }

        texture.generateMipmaps();
        Application.get().viewport(Display.getWidth(), Display.getHeight());

        // cull objects
        // cull 4 most important lights + 1 directional

        // for each face:

        // for each object:

        // setup lights
        // setup material parameters and textures
        // render object

        // end

        // rotate camera by 90°

        // end
    }
}
