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
package eu.matejkormuth.lpsim.gl;

import eu.matejkormuth.lpsim.DirectBufferPool;
import eu.matejkormuth.lpsim.Disposable;
import eu.matejkormuth.lpsim.Tagable;
import eu.matejkormuth.lpsim.math.Matrix4f;
import eu.matejkormuth.math.vectors.Vector2f;
import eu.matejkormuth.math.vectors.Vector3f;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public class Program implements Disposable, Tagable {

    // For optimization and error checking.
    private static int currentProgram;

    private final String name;
    private final int programId;
    private transient final List<Shader> shaders = new ArrayList<>(4);
    private final TObjectIntMap<String> uniformLocations = new TObjectIntHashMap<>();

    @Getter
    private String tag;

    public Program(String name) {
        this.name = name;
        // Create program.
        this.programId = GL20.glCreateProgram();
    }

    public void attachShader(Shader shader) {
        shaders.add(shader);
        GL20.glAttachShader(this.programId, shader.getShaderId());
    }

    public void link() {
        GL20.glLinkProgram(this.programId);

        String infoLog = GL20.glGetProgramInfoLog(this.programId, 8192);

        if (!infoLog.isEmpty()) {
            log.warn("Program linking info log:\n {}", infoLog.trim());
        }

        // Check for errors.
        if (GL20.glGetProgrami(this.programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Program linking failed!");
        }

        // Detach shaders.
        for (Shader shader : shaders) {
            GL20.glDetachShader(this.programId, shader.getShaderId());
        }
        shaders.clear();
    }

    public void validate() {
        GL20.glValidateProgram(this.programId);

        // Check for errors.
        if (GL20.glGetProgrami(this.programId, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
            String infolog = GL20.glGetProgramInfoLog(this.programId, 4096);
            log.error("Program validation error: {}", infolog.trim());

            throw new RuntimeException("Program validation failed!");
        }
    }

    public void setTag(String tag) {
        this.tag = tag;
        GL43.glObjectLabel(GL43.GL_PROGRAM, this.programId, "Program/" + tag);
    }

    private int getUniformLocation(String name) {
        if (uniformLocations.containsKey(name)) {
            return uniformLocations.get(name);
        } else {
            int location = GL20.glGetUniformLocation(this.programId, name);
            if (location == -1) {
                return -1;
                //throw new RuntimeException("Location of uniform " + name + " couldn't be found!");
            }
            uniformLocations.put(name, location);
            return location;
        }
    }

    // todo: on gl 4.1+ we have glProgramUniformXY()

    public Program setUniform(String name, float[] f) {
        ByteBuffer buff = DirectBufferPool.acquire(Float.BYTES * f.length);
        FloatBuffer fb = buff.asFloatBuffer();
        fb.put(f);
        fb.flip();
        checkCurrentProgram("set uniform");
        GL20.glUniform1(getUniformLocation(name), fb);
        DirectBufferPool.release(buff);
        return this;
    }

    public Program setUniform(String name, int[] f) {
        ByteBuffer buff = DirectBufferPool.acquire(Float.BYTES * f.length);
        IntBuffer fb = buff.asIntBuffer();
        fb.put(f);
        fb.flip();
        checkCurrentProgram("set uniform");
        GL20.glUniform1(getUniformLocation(name), fb);
        DirectBufferPool.release(buff);
        return this;
    }

    public Program setUniform(String name, float f) {
        checkCurrentProgram("set uniform");
        GL20.glUniform1f(getUniformLocation(name), f);
        return this;
    }

    public Program setUniform(String name, float f, float g) {
        checkCurrentProgram("set uniform");
        GL20.glUniform2f(getUniformLocation(name), f, g);
        return this;
    }

    public Program setUniform(String name, float f, float g, float h) {
        checkCurrentProgram("set uniform");
        GL20.glUniform3f(getUniformLocation(name), f, g, h);
        return this;
    }

    public Program setUniform(String name, float f, float g, float h, float i) {
        checkCurrentProgram("set uniform");
        GL20.glUniform4f(getUniformLocation(name), f, g, h, i);
        return this;
    }

    public Program setUniform(String name, int f) {
        checkCurrentProgram("set uniform");
        GL20.glUniform1i(getUniformLocation(name), f);
        return this;
    }

    public Program setUniform(String name, int f, int g) {
        checkCurrentProgram("set uniform");
        GL20.glUniform2i(getUniformLocation(name), f, g);
        return this;
    }

    public Program setUniform(String name, int f, int g, int h) {
        checkCurrentProgram("set uniform");
        GL20.glUniform3i(getUniformLocation(name), f, g, h);
        return this;
    }

    public Program setUniform(String name, int f, int g, int h, int i) {
        checkCurrentProgram("set uniform");
        GL20.glUniform4i(getUniformLocation(name), f, g, h, i);
        return this;
    }

    public Program setUniform(String name, Vector3f vector) {
        return setUniform(name, vector.getX(), vector.getY(), vector.getZ());
    }

    public Program setUniform(String name, Vector2f vector) {
        return setUniform(name, vector.getX(), vector.getY());
    }

    public Program setUniform(String name, Matrix4f matrix) {
        return setUniform(name, createMatrixBuffer(matrix));
    }

    // Not sure how safe this is, but it does reduce memory usage and GC.
    // Beware of multithreading!
    private static FloatBuffer sbuff;

    private FloatBuffer createMatrixBuffer(Matrix4f matrix) {
        if (sbuff == null) {
            sbuff = BufferUtils.createFloatBuffer(4 * 4);
        }
        sbuff.position(0);

        sbuff.put(matrix.m[0]);
        sbuff.put(matrix.m[1]);
        sbuff.put(matrix.m[2]);
        sbuff.put(matrix.m[3]);

        sbuff.flip();
        return sbuff;
    }

    public Program setUniform(String name, FloatBuffer matrix4) {
        checkCurrentProgram("set uniform");
        GL20.glUniformMatrix4(getUniformLocation(name), true, matrix4);
        return this;
    }

    public Program setUniform(String name, boolean val) {
        GL20.glUniform1i(getUniformLocation(name), val ? 1 : 0);
        return this;
    }

    private void checkCurrentProgram(String error) {
        if (this.programId != currentProgram) {
            throw new RuntimeException("Program is not active, can't: " + error);
        }
    }

    public Program use() {
        if (this.programId == currentProgram) {
            // Same program, we don't need this call.
            return this;
        }

        //log.info("USING PROGRAM {}", this.name);
        GL20.glUseProgram(this.programId);
        currentProgram = this.programId;
        return this;
    }

    @Override
    public void dispose() {
        GL20.glDeleteProgram(this.programId);
    }


}
