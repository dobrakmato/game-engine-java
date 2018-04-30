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
import lombok.RequiredArgsConstructor;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;

import javax.annotation.Nonnull;
import java.nio.FloatBuffer;

@RequiredArgsConstructor
public class InstancedFoliage extends WorldObject {

    /**
     * Default size of instance instanceBufferBuffer.
     */
    private static final int DEFAULT_MAX_INSTANCES = 1000 * 10;

    private static Program ALPHA_CUTOFF = ShaderCollection.provideProgram("grassGeometry");

    static {
        ALPHA_CUTOFF.use()
                .setUniform("map_albedo", 0)
                .setUniform("map_roughness", 1)
                .setUniform("map_normal", 3);
    }

    private final Material material;

    private final VAO vao;
    private final BufferObject vbo;
    private final BufferObject ibo;

    /*
     * vec3 position;
     * vec3 color;
     */
    private BufferObject instanceBufferObject;
    private FloatBuffer instanceBufferBuffer;

    private int indicesCount = 0;

    private int instanceCount = 0;
    private final int maxInstances;

    @Getter
    private boolean dynamic = true;
    private boolean bufferDirty = true;

    public InstancedFoliage(@Nonnull Mesh mesh, @Nonnull Material material) {
        this(mesh, material, DEFAULT_MAX_INSTANCES);
    }

    public InstancedFoliage(@Nonnull Mesh mesh, @Nonnull Material material, int maxInstances) {
        this.maxInstances = maxInstances;
        this.material = material;
        this.vbo = mesh.getVbo();
        this.ibo = mesh.getIbo();
        this.indicesCount = mesh.getIndicesCount();
        this.instanceBufferBuffer = BufferUtils.createFloatBuffer((4 * 4 + 3) * maxInstances);

        vao = new VAO();
        vao.bind();
        vao.setTag("InstancedFoliage " + this.toString());
        ibo.addReference();
        ibo.bindAsIbo();
        vbo.addReference();
        vbo.bindAsVbo();

        // Apply standard attributes.
        InterleavedVertexLayout.STANDARD.applyToBoundVAO();

        // Apply instanced attribues.
        instanceBufferObject = new BufferObject();
        instanceBufferObject.bindAsVbo();
        instanceBufferObject.allocate(BufferObjectTarget.ARRAY_BUFFER, Float.BYTES * (4 * 4 + 3), UsageHint.STREAM_DRAW);

        GL20.glEnableVertexAttribArray(5);
        GL20.glVertexAttribPointer(5, 4, GL11.GL_FLOAT, false, Float.BYTES * 19, 0);
        GL33.glVertexAttribDivisor(5, 1);

        GL20.glEnableVertexAttribArray(6);
        GL20.glVertexAttribPointer(6, 4, GL11.GL_FLOAT, false, Float.BYTES * 19, Float.BYTES * 4);
        GL33.glVertexAttribDivisor(6, 1);

        GL20.glEnableVertexAttribArray(7);
        GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, Float.BYTES * 19, Float.BYTES * 8);
        GL33.glVertexAttribDivisor(7, 1);

        GL20.glEnableVertexAttribArray(8);
        GL20.glVertexAttribPointer(8, 4, GL11.GL_FLOAT, false, Float.BYTES * 19, Float.BYTES * 12);
        GL33.glVertexAttribDivisor(8, 1);

        GL20.glEnableVertexAttribArray(9);
        GL20.glVertexAttribPointer(9, 3, GL11.GL_FLOAT, false, Float.BYTES * 19, Float.BYTES * 16);
        GL33.glVertexAttribDivisor(9, 1);


        vao.unbind();

        material.addReference();
        mesh.addReference();
    }

    public int addInstance(Matrix4f mat, Vector3f color) {
        if (this.instanceCount >= maxInstances) {
            throw new RuntimeException("Instance instanceBufferBuffer is full!");
        }

        int instanceId = instanceCount++;

        instanceBufferBuffer
                .put(mat.m[0])
                .put(mat.m[1])
                .put(mat.m[2])
                .put(mat.m[3])
                .put(color.getX())
                .put(color.getY())
                .put(color.getZ());

        bufferDirty = true;

        return instanceId;
    }

    public void clear() {
        this.instanceCount = 0;
        this.instanceBufferBuffer.clear();

        this.bufferDirty = true;
    }

    private void uploadUbo() {
        this.instanceBufferBuffer.flip();
        this.instanceBufferObject.bindAsArrayBuffer();
        this.instanceBufferObject.uploadData(BufferObjectTarget.ARRAY_BUFFER, instanceBufferBuffer, UsageHint.STREAM_DRAW);
        this.instanceBufferBuffer.flip();

        // This is currently broken in LWJGL.
        // this.instanceBufferObject.uploadData(BufferObjectTarget.ARRAY_BUFFER, (FloatBuffer) null, UsageHint.STREAM_DRAW);
        // this.instanceBufferObject.uploadSubData(BufferObjectTarget.ARRAY_BUFFER, 0, instanceBufferBuffer);
    }

    @Override
    public void dispose() {
        vao.dispose();
        material.removeReference();
        vbo.removeReference();
        ibo.removeReference();

        instanceBufferObject.dispose();
        instanceBufferBuffer = null;
    }

    @Override
    public void render(@Nonnull Camera camera) {
        ALPHA_CUTOFF.use()
                .setUniform("projection", camera.getProjectionMatrix())
                .setUniform("view", camera.getViewMatrix());


        material.setUniforms(ALPHA_CUTOFF);

        if (bufferDirty) {
            this.uploadUbo();
            bufferDirty = false;
        }

        if (ibo != null)
            drawElementsInstanced();
        else
            drawArraysInstanced();

    }

    private void drawElementsInstanced() {
        vao.bind();
        GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_INT, 0, instanceCount);
    }

    private void drawArraysInstanced() {
        throw new UnsupportedOperationException();
    }


    @RequiredArgsConstructor
    public static class FoliageMaterial extends Material {

        private final Texture2D albedo;
        private final Texture2D normal;
        private final Texture2D roughness = Texture2D.Util.WHITE;

        @Override
        public void dispose() {
            albedo.dispose();
            normal.dispose();
        }

        @Override
        public void setUniforms(Program program) {
            ALPHA_CUTOFF.setUniform("roughnessColor", 10.0f);

            Texture2D.activeSampler(1);
            roughness.bind();
            Texture2D.activeSampler(3);
            normal.bind();
            Texture2D.activeSampler(0);
            albedo.bind();
        }
    }
}
