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

import eu.matejkormuth.lpsim.Disposable;
import eu.matejkormuth.lpsim.Tagable;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Slf4j
public class Shader implements Disposable, Tagable {

    private final ShaderType type;
    private final String source;
    private final int shaderId;

    @Getter
    private String tag;

    public Shader(String source, ShaderType type) {
        this.source = source;
        this.type = type;

        // Create shader.
        this.shaderId = GL20.glCreateShader(type.getConstant());

        // Upload source.
        GL20.glShaderSource(this.shaderId, source);

        // Compile.
        GL20.glCompileShader(this.shaderId);

        long errorSize = GL20.glGetShaderi(this.shaderId, GL20.GL_INFO_LOG_LENGTH);
        String infoLog = GL20.glGetShaderInfoLog(this.shaderId, (int) errorSize).trim();

        if (!infoLog.isEmpty()) {
            log.warn("Shader compilation output: {}", infoLog.trim());
        }

        // Get errors.
        if (GL20.glGetShaderi(this.shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            log.error("Can't compile shader: ");

            TIntList erroredLines = new TIntArrayList();
            TIntList incidentLines = new TIntArrayList();
            Matcher matcher = Pattern.compile("\\d+\\((\\d+)\\)").matcher(infoLog); // "0(145)" -> 145 is a match
            while (matcher.find()) {
                int i = Integer.valueOf(matcher.group(1));
                if (i != 0) {
                    erroredLines.add(i - 4);
                    erroredLines.add(i - 3);
                    erroredLines.add(i - 2);
                    erroredLines.add(i - 1);
                    erroredLines.add(i);
                    erroredLines.add(i + 1);
                    erroredLines.add(i + 2);

                    incidentLines.add(i - 1);
                }
            }

            String[] lines = source.split("\n");

            int i = 0;
            boolean mode = false;
            boolean newMode = false;
            for (String line : lines) {
                if (erroredLines.contains(i)) {
                    newMode = true;
                    if (incidentLines.contains(i)) {
                        log.error("{} > {}", i, line);
                    } else {
                        log.info("{}   {}", i, line);
                    }
                } else {
                    newMode = false;
                }

                if (mode != newMode) {
                    log.info("...");
                    mode = newMode;
                }

                i++;
            }

            throw new RuntimeException("Can't compile shader!");
        }
    }

    public void setTag(String tag) {
        this.tag = tag;
        GL43.glObjectLabel(GL43.GL_SHADER, this.shaderId, "Shader/" + tag);
    }

    @Override
    public void dispose() {
        GL20.glDeleteShader(this.shaderId);
    }
}
