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
import eu.matejkormuth.lpsim.content.ShaderPreprocessor;
import eu.matejkormuth.lpsim.gl.Program;
import eu.matejkormuth.lpsim.gl.Shader;
import eu.matejkormuth.lpsim.gl.ShaderType;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class ShaderCollection {

    private static final Logger log = LoggerFactory.getLogger(ShaderCollection.class);

    private static Map<String, Shader> shaders = new HashMap<>();
    private static Map<String, Program> programs = new HashMap<>();

    public static void disposeAll() {
        log.info("Disposing all shaders and programs");
        for (Program program : programs.values()) {
            try {
                program.dispose();
            } catch (Exception e) {
                log.error("Can't dispose shader!", e);
            }
        }

        for (Shader shader : shaders.values()) {
            try {
                shader.dispose();
            } catch (Exception e) {
                log.error("Can't dispose shader!", e);
            }
        }
    }

    public static Shader provideShader(String name, ShaderType type) {
        return shaders.computeIfAbsent(name, s -> ShaderCollection.loadShader(name, type));
    }

    private static Shader loadShader(String name, ShaderType type) {
        log.info(" Loading shader {} of type {}...", name, type);
        log.info(" Shader file: {}", Content.getContent().resolve("shaders", name));
        return new Shader(runPreprocessor(Content.getContent().readText("shaders", name)), type);
    }

    private static String runPreprocessor(String input) {
        return ShaderPreprocessor.run(input);
    }

    public static Program provideProgram(String name) {
        return programs.computeIfAbsent(name, ShaderCollection::loadProgram);
    }

    private static Program loadProgram(String name) {
        log.info("Loading program {}...", name);
        Program program = new Program(name);
        program.setTag(name);
        program.attachShader(provideShader(name + ".vert", ShaderType.VERTEX));
        program.attachShader(provideShader(name + ".frag", ShaderType.FRAGMENT));
        if (Content.getContent().exists("shaders", name + ".tcs")) {
            program.attachShader(provideShader(name + ".tcs", ShaderType.TESSELLATION_CONTROL));
        }
        if (Content.getContent().exists("shaders", name + ".tes")) {
            program.attachShader(provideShader(name + ".tes", ShaderType.TESSELLATION_EVALUATION));
        }

        program.link();
        return program;
    }
}
